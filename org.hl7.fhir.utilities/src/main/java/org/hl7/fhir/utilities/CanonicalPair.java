package org.hl7.fhir.utilities;

public class CanonicalPair {

  private final String url;
  private final String version;
  
  public CanonicalPair(String target) {
    int pipeIndex = target != null ? target.indexOf('|') : -1;
    if (pipeIndex >= 0) {
      this.url = target.substring(0, pipeIndex);
      this.version = target.substring(pipeIndex+1);     
    } else {
      this.url = target;
      this.version = null;
    }
  }

  public String getUrl() {
    return url;
  }

  public String getVersion() {
    return version;
  }

}
