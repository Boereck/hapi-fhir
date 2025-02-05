/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package ca.uhn.hapi.fhir;

import java.util.List;

import org.hl7.fhir.common.hapi.validation.support.ResourceAggregator;
import org.hl7.fhir.common.hapi.validation.support.ResourceAggregatorSynchronized;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain.CacheConfiguration;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;

@State(Scope.Thread)
public class MyBenchmark {

  private ValidationSupportChain chainSynchronized;
  private ValidationSupportChain chainAtomic;

  @Setup
  public void setup() {
    FhirContext ctx = FhirContext.forR5();
    DefaultProfileValidationSupport defaultProfile = new DefaultProfileValidationSupport(ctx);
    chainSynchronized =
        new ValidationSupportChain(
            CacheConfiguration.defaultValues(),
            List.of(defaultProfile),
            ResourceAggregatorSynchronized::new);
    chainAtomic =
        new ValidationSupportChain(
            CacheConfiguration.defaultValues(), List.of(defaultProfile), ResourceAggregator::new);
  }

  @Benchmark
  @OperationsPerInvocation(10_000)
  public void testSynchronized(Blackhole bh) {
    performFetchAll(chainSynchronized, bh);
  }

  @Benchmark
  @OperationsPerInvocation(10_000)
  public void testAtomic(Blackhole bh) {
    performFetchAll(chainAtomic, bh);
  }

  private void performFetchAll(ValidationSupportChain chain, Blackhole bh) {
    for (int i = 0; i < 10_000; i++) {
      bh.consume(chain.fetchAllStructureDefinitions());
    }
  }
}
