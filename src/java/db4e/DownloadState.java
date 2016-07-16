package db4e;

import java.util.function.Consumer;

class DownloadState {
   volatile int downloaded;
   volatile int total;
   volatile boolean isCategoryComplete;

   private final Consumer<Double> updater;

   DownloadState ( Consumer<Double> updater ) {
      this.updater = updater;
   }

   void update () {
      updater.accept( getProgress() );
   }

   private double getProgress() {
      if ( total <= 0 ) return 0;
      if ( downloaded >= total ) return 1;
      return downloaded / (double) total;
   }
}