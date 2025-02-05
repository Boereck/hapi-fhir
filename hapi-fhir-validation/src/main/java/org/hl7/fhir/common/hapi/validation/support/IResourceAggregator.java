package org.hl7.fhir.common.hapi.validation.support;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IResourceAggregator {

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
	public List<IBaseResource> computeAllIfAbsent(
			FhirContext ctx, Supplier<? extends List<IBaseResource>> listSupplier);

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
	public IBaseResource computeIfAbsent(
			FhirContext ctx, String url, Function<? super String, ? extends IBaseResource> mapperFunction);

	/**
	 * Clears all previously aggregated resources.
	 */
	public void clear();

	/**
	 * Returns the amount of currently aggregated resources.
	 * @return number of resources aggregated until now
	 */
	public int size();
}
