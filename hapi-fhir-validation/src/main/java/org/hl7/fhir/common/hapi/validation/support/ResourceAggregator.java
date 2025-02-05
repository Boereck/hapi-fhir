package org.hl7.fhir.common.hapi.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.FhirTerser;
import jakarta.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * This class aggregates resources computed/looked up by user provided actions in a thread safe manner.<br>
 * The class allows to specify via {@link ResourceAggregator#ResourceAggregator(boolean) constructor}
 * that resources are not actually aggregated, or always computed/looked up. If there shall be no aggregation,
 * the requested resource(s) are always regarded missing from the aggregation and computed on request.<br><br>
 * It is assumed that all resources added via {@link #computeIfAbsent(FhirContext, String, Function)}
 * are also in the list added via {@link #computeAllIfAbsent(FhirContext, Supplier)} (if called at all).
 * It is considered undefined behavior if e.g. {@code computeAllIfAbsent} was
 * called and a list of resources was added and afterwards {@link #computeIfAbsent}
 * is called and a resource not part of the initially added list of resources is aggregated.
 */
public class ResourceAggregator implements IResourceAggregator {

	private final boolean actuallyStore;

	/**
	 * Creates a new {@code ResourceAggregator} instance. The parameter {@code actuallyStore}
	 * determines, if computed resources are actually aggregated. If the parameter is {@code false},
	 * the aggregator will always remain empty and resources are always computed and never look up
	 * in the aggregation.
	 * @param actuallyStore determines if resources are actually aggregated or always computed.
	 */
	public ResourceAggregator(boolean actuallyStore) {
		this.actuallyStore = actuallyStore;
	}

	/**
	 * Holds consistent state of all aggregated resources
	 */
	private AtomicReference<ResourcesHolder> holder = new AtomicReference<>(ResourcesHolder.EMPTY);

	/**
	 * This class holds previously found resources plus the status if
	 * all found resources were already added in bulk.
	 */
	private static final class ResourcesHolder {

		static final ResourcesHolder EMPTY = new ResourcesHolder();

		@Nonnull
		private final Map<String, IBaseResource> resourcesByUrl;

		@Nonnull
		private final List<IBaseResource> resourcesAsList;

		private final boolean loadedAllResources;

		private ResourcesHolder() {
			resourcesByUrl = Map.of();
			resourcesAsList = List.of();
			loadedAllResources = false;
		}

		private ResourcesHolder(
				Map<String, IBaseResource> resourcesByUrl,
				List<IBaseResource> resourcesAsList,
				boolean loadedAllStructDefs) {
			this.resourcesByUrl = resourcesByUrl;
			this.resourcesAsList = resourcesAsList;
			this.loadedAllResources = loadedAllStructDefs;
		}

		public ResourcesHolder withAllStructureDefinitions(FhirContext ctx, List<IBaseResource> resources) {
			return withStructureDefinitions(ctx, resources, true);
		}

		public ResourcesHolder withStructureDefinition(FhirContext ctx, IBaseResource resource) {
			return withStructureDefinitions(ctx, List.of(resource), this.loadedAllResources);
		}

		private ResourcesHolder withStructureDefinitions(FhirContext ctx, List<IBaseResource> resources, boolean all) {
			List<IBaseResource> resultList = new ArrayList<>(resourcesAsList);
			Map<String, IBaseResource> resultMap = new HashMap<>(resourcesByUrl);
			FhirTerser terser = ctx.newTerser();
			// if this holder already contained all resources, but loadedAllStructDefs flag
			// was not set to true, we consider it a change
			boolean changed = !this.loadedAllResources && all;
			for (IBaseResource res : resources) {
				String url = getUrl(terser, res);
				IBaseResource prev = resultMap.putIfAbsent(url, res);
				if (prev == null) {
					changed = true;
					resultList.add(res);
				}
			}
			boolean allNow = this.loadedAllResources || all;
			return changed ? new ResourcesHolder(resultMap, resultList, allNow) : this;
		}

		private static String getUrl(FhirTerser terser, IBaseResource res) {
			String url = terser.getSinglePrimitiveValueOrNull(res, "url");
			url = defaultIfBlank(url, UUID.randomUUID().toString());
			return url;
		}

		public List<IBaseResource> allResources() {
			return Collections.unmodifiableList(resourcesAsList);
		}

		public IBaseResource get(String url) {
			return resourcesByUrl.get(url);
		}

		public int size() {
			return resourcesAsList.size();
		}

		public boolean allLoaded() {
			return this.loadedAllResources;
		}
	}

	/**
	 * Returns previously (via this method) aggregated resources, or computes resources from {@code listSupplier},
	 * if resources were not aggregated before. If this aggregator actually aggregates
	 * (see {@link ResourceAggregator#ResourceAggregator(boolean) constructor}) the computed resources will be
	 * stored internally and returned on the next call to this method.<br><br>
	 * Implementation note: The returned list represents a snapshot in time and will not change, even if resources
	 * are added to the aggregator concurrently or the aggregator is cleared.
	 * @param ctx the FHIR context appropriate for the resources
	 * @param listSupplier supplier used to compute/fetch resources not aggregated before. The supplier must not
	 *  be {@code null}, and the returned list must also not be {@code null}.
	 * @return list of all aggregated or computed resources
	 */
	@Override
	public List<IBaseResource> computeAllIfAbsent(
			FhirContext ctx, Supplier<? extends List<IBaseResource>> listSupplier) {
		if (!actuallyStore) {
			return listSupplier.get();
		}
		// did we already store all StructureDefinitions?
		ResourcesHolder oldHolder = holder.get();
		if (oldHolder.allLoaded()) {
			return oldHolder.allResources();
		}
		// Do retrieve and store all StructureDefinitions
		List<IBaseResource> list = listSupplier.get();
		while (true) {
			ResourcesHolder newHolder = oldHolder.withAllStructureDefinitions(ctx, list);
			// we can omit checking if newHolder == oldHolder. This cannot be, since we checked
			// oldHolder.allLoaded() earlier, to check for the case that all SDs were already added
			ResourcesHolder witness = holder.compareAndExchange(oldHolder, newHolder);
			if (witness == oldHolder) {
				return newHolder.allResources();
			} else {
				// holder changed in the meantime,
				oldHolder = witness;
			}
		}
	}

	/**
	 * Returns previously aggregated resource with the given {@code url} or computes the resource
	 * from {@code mapperFunction}, if the resource with the given {@code url} was not aggregated before.
	 * If this aggregator actually aggregates
	 * (see {@link ResourceAggregator#ResourceAggregator(boolean) constructor}) the computed resource will be
	 * stored internally and can be looked up from the aggregation on another call with the same {@code url}.
	 * @param ctx the FHIR context appropriate for the resource
	 * @param url the URL identifying the resource to look up (or compute).
	 * @param mapperFunction the function being called with {@code url}, if the resource identified by the
	 *  URL was not aggregated before. This parameter must not be {@code null}, object returned from
	 *  the function can be {@code null}. In this case this method will return {@code null} as well and
	 *  not aggregate any value.
	 * @return the resource with the given {@code url}, or {@code null} if no such resource can be found or
	 *  computed
	 */
	@Override
	public IBaseResource computeIfAbsent(
			FhirContext ctx, String url, Function<? super String, ? extends IBaseResource> mapperFunction) {
		if (!actuallyStore) {
			return mapperFunction.apply(url);
		}
		IBaseResource candidate = holder.get().get(url);
		if (candidate != null) {
			return candidate;
		}
		candidate = mapperFunction.apply(url);
		if (candidate == null) {
			return null;
		}
		add(ctx, candidate);
		return candidate;
	}

	/**
	 * Clears all previously aggregated resources.
	 */
	@Override
	public void clear() {
		if (!actuallyStore) {
			return;
		}
		holder.set(ResourcesHolder.EMPTY);
	}

	private void add(FhirContext ctx, IBaseResource res) {
		ResourcesHolder oldHolder = holder.get();
		while (true) {
			ResourcesHolder newHolder = oldHolder.withStructureDefinition(ctx, res);
			// if nothing changed, resource was already added
			if (newHolder == oldHolder) {
				return;
			}
			ResourcesHolder witness = holder.compareAndExchange(oldHolder, newHolder);
			// check if we succeeded setting the new holder
			if (witness == oldHolder) {
				return;
			} else {
				// holder changed in the meantime, try adding our resource once again
				oldHolder = witness;
			}
		}
	}

	/**
	 * Returns the amount of currently aggregated resources.
	 * @return number of resources aggregated until now
	 */
	@Override
	public int size() {
		if (!actuallyStore) {
			return 0;
		}
		return holder.get().size();
	}
}
