package org.hl7.fhir.common.hapi.validation.support;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.FhirTerser;

class ResourceAggregatorTest {

	private static final Supplier<List<IBaseResource>> FAIL_ALL = () -> fail("Shall not be called on aggregation.");
	private static final Function<String,IBaseResource> FAIL = url -> fail("Shall not be called on aggregation.");
	private static final String DEMO_URL = "http://foobar";
	private static final String DEMO_URL_2 = "http://barfoo";
	private static final FhirContext ctx = FhirContext.forR5Cached();

	@Test
	void testComputeWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkCompute(aggregator);
	}

	@Test
	void testComputeWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkCompute(aggregator);
	}

	private void checkCompute(ResourceAggregator aggregator){
		StructureDefinition sd = new StructureDefinition().setUrl(DEMO_URL);
		IBaseResource result = aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd);
		assertSame(sd, result);
	}

	@Test
	void testComputeWithAggregationTwice() {
		var aggregator = new ResourceAggregator(true);
		StructureDefinition sd = new StructureDefinition().setUrl(DEMO_URL);
		IBaseResource result = aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd);
		IBaseResource result2 = aggregator.computeIfAbsent(ctx, DEMO_URL, FAIL);
		assertSame(sd, result);
		assertSame(result, result2);
	}

	@Test
	void testComputeWithAggregationCheckUrl() {
		var aggregator = new ResourceAggregator(true);
		checkComputeUrl(aggregator);
	}

	@Test
	void testComputeWithoutAggregationCheckUrl() {
		var aggregator = new ResourceAggregator(false);
		checkComputeUrl(aggregator);
	}

	private void checkComputeUrl(ResourceAggregator aggregator) {
		StructureDefinition sd = new StructureDefinition().setUrl(DEMO_URL);
		String[] recordedUrl = { null };
		
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> {
			recordedUrl[0] = url;
			return sd;
		});
		
		assertSame(DEMO_URL, recordedUrl[0]);
	}

	@Test
	void testComputeWithoutAggregationTwice() {
		var aggregator = new ResourceAggregator(false);
		StructureDefinition sd = new StructureDefinition().setUrl(DEMO_URL);
		IBaseResource result = aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd);
		IBaseResource result2 = aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd);
		assertSame(sd, result);
		assertSame(result, result2);
	}

	@Test
	void testComputeAllWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkComputeAll(aggregator);
	}

	@Test
	void testComputeAllWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkComputeAll(aggregator);
	}

	private void checkComputeAll(ResourceAggregator aggregator) {
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1, sd2);
		List<IBaseResource> result = aggregator.computeAllIfAbsent(ctx, () -> resources);
		assertEquals(resources, result);
	}

	@Test
	void testComputeAllTwiceWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1, sd2);
		List<IBaseResource> result1 = aggregator.computeAllIfAbsent(ctx, () -> resources);
		List<IBaseResource> result2 = aggregator.computeAllIfAbsent(ctx, FAIL_ALL);
		assertEquals(resources, result1);
		assertEquals(result1, result2);
	}

	@Test
	void testComputeAllTwiceWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1, sd2);
		List<IBaseResource> result1 = aggregator.computeAllIfAbsent(ctx, () -> resources);
		List<IBaseResource> result2 = aggregator.computeAllIfAbsent(ctx, () -> resources);
		assertEquals(resources, result1);
		assertEquals(result1, result2);
	}

	@Test
	void testComputeAfterComputeAllWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1, sd2);
		aggregator.computeAllIfAbsent(ctx, () -> resources);
		IBaseResource result = aggregator.computeIfAbsent(ctx, DEMO_URL, FAIL);
		assertSame(sd1, result);
	}

	@Test
	void testComputeAfterComputeAllWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1, sd2);
		aggregator.computeAllIfAbsent(ctx, () -> resources);
		IBaseResource result = aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd1);
		assertSame(sd1, result);
	}

	@Test
	void testAddingOneByOneThenAddAll() {
		var aggregator = new ResourceAggregator(true);

		String urlBase = "http://foobar/";
		int resCount = 100;
		List<StructureDefinition> structDefs = IntStream.range(0,resCount)
				.mapToObj(j -> urlBase + j)
				.map(url -> new StructureDefinition().setUrl(url))
				.collect(toList());
		@SuppressWarnings({"rawtypes", "unchecked"})
		List<IBaseResource> resources = (List)structDefs;
		
		List<StructureDefinition> reversed = new ArrayList<>(structDefs);
		Collections.reverse(reversed);
		for(StructureDefinition sd : reversed) {
			aggregator.computeIfAbsent(ctx, sd.getUrl(), url -> sd);
		}
		var result = aggregator.computeAllIfAbsent(ctx, () -> resources);
		assertThat(result).containsExactlyInAnyOrderElementsOf(structDefs);
	}

	@Test
	void testAddingOneByOnePlusOneThenAddAll() {
		// This actually tests behavior declared undefined behavior by the API.
		// However, we want to provide a sensible implementation, doing what 
		// most users would expect.
		var aggregator = new ResourceAggregator(true);
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		StructureDefinition sdAdditional = new StructureDefinition().setUrl("http://unexpected");
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd1);
		aggregator.computeIfAbsent(ctx, DEMO_URL_2, url -> sd2 );
		aggregator.computeIfAbsent(ctx, sdAdditional.getUrl(), url -> sdAdditional);

		List<IBaseResource> resources = List.of(sd1, sd2);
		var compAllResult = aggregator.computeAllIfAbsent(ctx, () -> resources);

		assertThat(compAllResult).containsExactlyInAnyOrder(sd1, sd2, sdAdditional);
	}

	@Test
	void testAddAllThenAddingOneByOnePlusOne() {
		// This actually tests behavior declared undefined behavior by the API.
		// However, we want to provide a sensible implementation, doing what 
		// most users would expect.
		var aggregator = new ResourceAggregator(true);
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1, sd2);
		StructureDefinition sdAdditional = new StructureDefinition().setUrl("http://unexpected");
		aggregator.computeAllIfAbsent(ctx, () -> resources);
		aggregator.computeIfAbsent(ctx, DEMO_URL, FAIL);
		aggregator.computeIfAbsent(ctx, DEMO_URL_2, FAIL);
		var resultAdditional = aggregator.computeIfAbsent(ctx, sdAdditional.getUrl(), url -> sdAdditional);
		assertSame(sdAdditional, resultAdditional);
		var compAllResult = aggregator.computeAllIfAbsent(ctx, () -> resources);
		assertThat(compAllResult).containsExactlyInAnyOrder(sd1, sd2, sdAdditional);
	}

	@Test
	void testComputeAllAfterComputeWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkComputeAllAfterCompute(aggregator);
	}

	@Test
	void testComputeAllAfterComputeWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkComputeAllAfterCompute(aggregator);
	}

	private void checkComputeAllAfterCompute(ResourceAggregator aggregator) {
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1, sd2);

		aggregator.computeIfAbsent(ctx, DEMO_URL_2, url -> sd2);
		var result = aggregator.computeAllIfAbsent(ctx, () -> resources);
		assertThat(result).containsExactlyInAnyOrderElementsOf(resources);
	}

	@Test
	void testComputeIfAbsentReturnNullWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkComputeIfAbsentReturnNull(aggregator);
	}

	@Test
	void testComputeIfAbsentReturnNullWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkComputeIfAbsentReturnNull(aggregator);
	}

	private void checkComputeIfAbsentReturnNull(ResourceAggregator aggregator) {
		IBaseResource result = aggregator.computeIfAbsent(ctx, DEMO_URL, url -> null);
		assertNull(result);
		assertEquals(0, aggregator.size());
	}

	@Test
	void testClearEmptyWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkClearNoThrow(aggregator);
	}

	@Test
	void testClearEmptyWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkClearNoThrow(aggregator);
	}

	private void checkClearNoThrow(ResourceAggregator aggregator) {
		Assertions.assertThatNoException().isThrownBy(aggregator::clear);
	}

	@Test
	void testClearAfterComputeWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkClearAfterCompute(aggregator);
	}

	@Test
	void testClearAfterComputeWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkClearAfterCompute(aggregator);
	}
	
	private void checkClearAfterCompute(ResourceAggregator aggregator) {
		StructureDefinition sd = new StructureDefinition().setUrl(DEMO_URL);
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd);
		aggregator.clear();
		assertEquals(0, aggregator.size());
	}
	
	@Test
	void testClearAfterComputeTwiceWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkClearAfterComputeTwice(aggregator);
	}

	@Test
	void testClearAfterComputeTwiceWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkClearAfterComputeTwice(aggregator);
	}

	private void checkClearAfterComputeTwice(ResourceAggregator aggregator) {
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd1);
		aggregator.computeIfAbsent(ctx, DEMO_URL_2, url -> sd2);
		aggregator.clear();
		assertEquals(0, aggregator.size());
	}

	@Test
	void testClearAfterComputeAllWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkClearAfterComputeAll(aggregator);
	}

	@Test
	void testClearAfterComputeAllWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkClearAfterComputeAll(aggregator);
	}
	
	private void checkClearAfterComputeAll(ResourceAggregator aggregator) {
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1,sd2);
		aggregator.computeAllIfAbsent(ctx, () -> resources);
		aggregator.clear();
		assertEquals(0, aggregator.size());
	}

	@Test
	void testSizeEmptyWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		assertEquals(0, aggregator.size());
	}

	@Test
	void testSizeEmptyWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		assertEquals(0, aggregator.size());
	}

	@Test
	void testSizeAfterComputeWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd1);
		assertEquals(1, aggregator.size());
	}

	@Test
	void testSizeAfterComputeWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd1);
		assertEquals(0, aggregator.size());
	}

	@Test
	void testSizeAfterComputeTwiceSameWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkSizeAfterComputeTwiceSame(aggregator, 1);
	}

	@Test
	void testSizeAfterComputeTwiceSameWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkSizeAfterComputeTwiceSame(aggregator, 0);
	}
	
	private void checkSizeAfterComputeTwiceSame(ResourceAggregator aggregator, int expectedSize) {
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd1);
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd1);
		assertEquals(expectedSize, aggregator.size());
	}
	
	@Test
	void testSizeAfterComputeTwiceWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkSizeAfterComputeTwice(aggregator, 2);
	}

	@Test
	void testSizeAfterComputeTwiceWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkSizeAfterComputeTwice(aggregator, 0);
	}

	private void checkSizeAfterComputeTwice(ResourceAggregator aggregator, int expectedSize) {
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		aggregator.computeIfAbsent(ctx, DEMO_URL, url -> sd1);
		aggregator.computeIfAbsent(ctx, DEMO_URL_2, url -> sd2);
		assertEquals(expectedSize, aggregator.size());
	}

	@Test
	void testSizeAfterComputeAllWithAggregation() {
		var aggregator = new ResourceAggregator(true);
		checkSizeAfterComputeAll(aggregator, 2);
	}

	@Test
	void testSizeAfterComputeAllWithoutAggregation() {
		var aggregator = new ResourceAggregator(false);
		checkSizeAfterComputeAll(aggregator, 0);
	}
	
	private void checkSizeAfterComputeAll(ResourceAggregator aggregator, int expectedSize) {
		StructureDefinition sd1 = new StructureDefinition().setUrl(DEMO_URL);
		StructureDefinition sd2 = new StructureDefinition().setUrl(DEMO_URL_2);
		List<IBaseResource> resources = List.of(sd1,sd2);
		aggregator.computeAllIfAbsent(ctx, () -> resources);
		assertEquals(expectedSize, aggregator.size());
	}

	@Test
	void testConcurrentAdds() {
		// Unfortunately this test is not deterministic, but it tests interaction
		// while concurrently adding resources to the aggregator.
		ForkJoinPool commonPool = ForkJoinPool.commonPool();
		
		FhirTerser terser = ctx.newTerser();
		
		int count = 10_000;
		var aggregator = new ResourceAggregator(true);
		Runnable addTillTenThousand = () -> {
			String urlBase = "http://foobar/";
			for(int i=0; i<count; i++) {
				String url = urlBase + i;
				StructureDefinition sd = new StructureDefinition().setUrl(url);
				var result = aggregator.computeIfAbsent(ctx, url, u -> sd);
				assertNotNull(result);
				Optional<String> resultUrlOpt = terser.getSinglePrimitiveValue(sd, "url");
				assertTrue(resultUrlOpt.isPresent());
				assertEquals(url, resultUrlOpt.get());
			}
		};
		
		var one = commonPool.submit(addTillTenThousand);
		var two = commonPool.submit(addTillTenThousand);
		
		one.join();
		two.join();
		
		assertEquals(count, aggregator.size());
	}

	@Test
	void testConcurrentAddAll() {
		// Unfortunately this test is not deterministic, but it tests interaction
		// while concurrently adding all resources to the aggregator.
		ForkJoinPool commonPool = ForkJoinPool.commonPool();
		
		String urlBase = "http://foobar/";
		List<IBaseResource> resources = IntStream.range(0,100)
				.mapToObj(j -> urlBase + j)
				.map(url -> (IBaseResource) new StructureDefinition().setUrl(url))
				.collect(toList());
		int count = 10_000;
		for(int i=0; i<count; i++) {
			var aggregator = new ResourceAggregator(true);
			Runnable addAll = () -> {
					var result = aggregator.computeAllIfAbsent(ctx, () -> resources);
					assertEquals(resources, result);
			};
			var one = commonPool.submit(addAll);
			var two = commonPool.submit(addAll);
			one.join();
			two.join();
		}
	}

	@Test
	void testConcurrentAddAndAddAll() {
		// Unfortunately this test is not deterministic, but it tests interaction
		// while concurrently adding all resources at onece and resources sequentially to the aggregator.
		ForkJoinPool commonPool = ForkJoinPool.commonPool();
		
		String urlBase = "http://foobar/";
		int resCount = 100;
		List<StructureDefinition> structDefs = IntStream.range(0,resCount)
				.mapToObj(j -> urlBase + j)
				.map(url -> new StructureDefinition().setUrl(url))
				.collect(toList());
		@SuppressWarnings({"rawtypes", "unchecked"})
		List<IBaseResource> resources = (List)structDefs;

		for(int i=0; i<1_000; i++) {
			var aggregator = new ResourceAggregator(true);
			Runnable addAll = () -> {
					var result = aggregator.computeAllIfAbsent(ctx, () -> resources);
					assertEquals(resources, result);
			};
			Runnable add = () -> {
				for(StructureDefinition res : structDefs) {
					var result = aggregator.computeIfAbsent(ctx, res.getUrl(), url -> res);
					assertSame(res, result);
				}
			};

			var one = commonPool.submit(add);
			var two = commonPool.submit(addAll);
			one.join();
			two.join();

			assertEquals(resCount, aggregator.size());
		}
	}
}
