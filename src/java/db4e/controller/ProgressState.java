package db4e.controller;

import java.util.function.Consumer;

public class ProgressState {
   public volatile int done;
   public volatile int total;

   private final Consumer<Double> updater;

   ProgressState ( Consumer<Double> updater ) {
      this.updater = updater;
   }

   public void addOne () {
      if ( ++done % 1024 == 0 )
         update();
   }

   public void update () {
      updater.accept( getProgress() );
   }

   private double getProgress() {
      if ( total <= 0 ) return 0;
      if ( done >= total ) return 1;
      return done / (double) total;
   }
}