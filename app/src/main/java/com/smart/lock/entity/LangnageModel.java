
package com.smart.lock.entity;

import java.io.Serializable;

/**
 */
public class LangnageModel implements Serializable {

   private String langnage;

   private boolean defaultLangnage;

   public String getLangnage() {
      return langnage;
   }

   public void setLangnage(String langnage) {
      this.langnage = langnage;
   }

   public boolean isDefaultLangnage() {
      return defaultLangnage;
   }

   public void setDefaultLangnage(boolean defaultLangnage) {
      this.defaultLangnage = defaultLangnage;
   }
}
