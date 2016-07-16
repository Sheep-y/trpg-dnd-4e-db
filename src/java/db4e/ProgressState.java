package db4e;

import java.util.function.Consumer;

class ProgressState {
   volatile int done;
   volatile int total;

   private final Consumer<Double> updater;

   ProgressState ( Consumer<Double> updater ) {
      this.updater = updater;
   }

   void update () {
      updater.accept( getProgress() );
   }

   private double getProgress() {
      if ( total <= 0 ) return 0;
      if ( done >= total ) return 1;
      return done / (double) total;
   }
}