(function Base85( root ) {


   var ENCODABET = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~';
   var DECODABET = [];
   for( var i= 0, len = ENCODABET.length ; i < len ; ++i )
      DECODABET[ ENCODABET.charCodeAt( i ) ] = i;

   var FACTORS= [
      1,
      85,
      7225,     // 85^2
      614125,   // 85^3
      52200625  // 85^4
   ];

   root.Base85 = {
      decode: function base85_decode ( input ) {
         var b, factor = 5, sum = 0, out_count = 0;
         var out = new Uint8Array( Math.ceil( input.length * 0.8 ) + 4 );
         for( var i= 0, len = input.length ; i < len ; ++i ) {
            sum += DECODABET[ input.charCodeAt( i )] * FACTORS[--factor];
            if ( factor === 0 ) {
               for ( var e= 24; e >= 0; e-= 8)
                  out[ out_count++ ] = (sum >>> e) & 0xFF;
               sum= 0;
               factor= 5;
            }
         }
         // process rest (if present)
         if ( factor < 5 ) {
            sum/= FACTORS[factor];
            for( var e= (3 - factor) * 8; e >= 0; e-= 8 )
               out[ out_count++ ] = (sum >>> e) & 0xFF;
         }
         if ( out_count !== out.length )
            return new Uint8Array( out, 0, out_count );
         return out;
      }
   };

})( this || window || global );