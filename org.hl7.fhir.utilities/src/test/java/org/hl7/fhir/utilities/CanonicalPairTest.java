package org.hl7.fhir.utilities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CanonicalPairTest {

  @Test
  void testCanonicalNull() {
    var canonical = new CanonicalPair(null);
    assertNull(canonical.getUrl());
    assertNull(canonical.getVersion());
  }
  
  @Test
  void testCanonicalEmpty() {
    var url = "";
    var canonical = new CanonicalPair(url);
    assertEquals(url, canonical.getUrl());
    assertNull(canonical.getVersion());
  }
  
  @Test
  void testCanonicalWithoutVersion() {
    var url = "https://www.test.org";
    var canonical = new CanonicalPair(url);
    assertEquals(url, canonical.getUrl());
    assertNull(canonical.getVersion());
  }
  
  @Test
  void testCanonicalWithEmptyVersion() {
    var expectedUrl = "https://www.test.org";
    var url = expectedUrl + "|";
    var canonical = new CanonicalPair(url);
    assertEquals(expectedUrl, canonical.getUrl());
    assertEquals("", canonical.getVersion());
  }

  @Test
  void testCanonicalWithVersion() {
    var expectedUrl = "https://www.test.org";
    var expectedVersion = "2.6";
    var url = expectedUrl + "|" + expectedVersion;
    var canonical = new CanonicalPair(url);
    assertEquals(expectedUrl, canonical.getUrl());
    assertEquals(expectedVersion, canonical.getVersion());
  }

  @Test
  void testCanonicalWithVersionIncludingPipe() {
    var expectedUrl = "https://www.test.org";
    var expectedVersion = "2024|05";
    var url = expectedUrl + "|" + expectedVersion;
    var canonical = new CanonicalPair(url);
    assertEquals(expectedUrl, canonical.getUrl());
    assertEquals(expectedVersion, canonical.getVersion());
  }
  
}
