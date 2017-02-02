package db4e.controller;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ProgressState {
   private final AtomicInteger done = new AtomicInteger( 0 );
   public volatile int total;

   private final Consumer<Double> updater;

   ProgressState ( Consumer<Double> updater ) {
      this.updater = updater;
   }

   public void reset () {
      done.set( 0 );
      update();
   }

   public void add ( int i ) {
      done.addAndGet( i );
      update();
   }

   public int get () {
      return done.get();
   }

   public void set ( int i ) {
      done.set( i );
      update();
   }

   public void addOne () {
      if ( done.incrementAndGet() % 256 == 0 )
         update();
   }

   public void update () {
      updater.accept( getProgress() );
   }

   private double getProgress() {
      if ( total <= 0 ) return 0;
      if ( done.get() >= total ) return 1;
      return done.get() / (double) total;
   }
}