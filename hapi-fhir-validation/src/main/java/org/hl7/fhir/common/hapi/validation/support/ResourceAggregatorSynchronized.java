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
import java.util.function.Function;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class ResourceAggregatorSynchronized implements IResourceAggregator {

	/**
	 * See class documentation for an explanation of why this is separate
	 * and non-expiring. Note that this field is non-synchronized. If you
	 * access it, you should first wrap the call in
	 * <code>synchronized(myStructureDefinitionsByUrl)</code>.
	 */
	@Nonnull
	private final Map<String, IBaseResource> myStructureDefinitionsByUrl = new HashMap<>();
	/**
	 * See class documentation for an explanation of why this is separate
	 * and non-expiring. Note that this field is non-synchronized. If you
	 * access it, you should first wrap the call in
	 * <code>synchronized(myStructureDefinitionsByUrl)</code> (synchronize on
	 * the other field because both collections are expected to be modified
	 * at the same time).
	 */
	@Nonnull
	private final List<IBaseResource> myStructureDefinitionsAsList = new ArrayList<>();

	private volatile boolean myHaveFetchedAllStructureDefinitions = false;

	private final boolean doCache;

	public ResourceAggregatorSynchronized(boolean doCache) {
		this.doCache = doCache;
	}

	@Override
	public List<IBaseResource> computeAllIfAbsent(
			FhirContext ctx, Supplier<? extends List<IBaseResource>> listSupplier) {
		if (!myHaveFetchedAllStructureDefinitions) {
			FhirTerser terser = ctx.newTerser();
			List<IBaseResource> allStructureDefinitions = listSupplier.get();
			if (doCache) {
				synchronized (myStructureDefinitionsByUrl) {
					for (IBaseResource structureDefinition : allStructureDefinitions) {
						String url = terser.getSinglePrimitiveValueOrNull(structureDefinition, "url");
						url = defaultIfBlank(url, UUID.randomUUID().toString());
						if (myStructureDefinitionsByUrl.putIfAbsent(url, structureDefinition) == null) {
							myStructureDefinitionsAsList.add(structureDefinition);
						}
					}
				}
			}
			myHaveFetchedAllStructureDefinitions = true;
		}
		return Collections.unmodifiableList(new ArrayList<>(myStructureDefinitionsAsList));
	}

	@Override
	public IBaseResource computeIfAbsent(
			FhirContext ctx, String url, Function<? super String, ? extends IBaseResource> mapperFunction) {
		synchronized (myStructureDefinitionsByUrl) {
			IBaseResource candidate = myStructureDefinitionsByUrl.get(url);
			if (candidate == null) {
				candidate = mapperFunction.apply(url);
				if (doCache && candidate != null) {
					if (myStructureDefinitionsByUrl.putIfAbsent(url, candidate) == null) {
						myStructureDefinitionsAsList.add(candidate);
					}
				}
			}
			return candidate;
		}
	}

	@Override
	public void clear() {
		myHaveFetchedAllStructureDefinitions = false;
		synchronized (myStructureDefinitionsByUrl) {
			myStructureDefinitionsByUrl.clear();
			myStructureDefinitionsAsList.clear();
		}
	}

	@Override
	public int size() {
		return myStructureDefinitionsAsList.size();
	}
}
