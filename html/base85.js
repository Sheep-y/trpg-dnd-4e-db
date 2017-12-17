(function Base85 ( root ) {

/* Base85 (RFC 1924) decoder */

var ENCODABET = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~';
var DECODABET = [,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,62,,63,64,65,66,,67,68,69,70,,71,,,0,1,2,3,4,5,6,7,8,9,,72,73,74,75,76,77,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,,,,78,79,80,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,81,82,83,84];
/* for ( var i= 0, len = ENCODABET.length ; i < len ; ++i )
      DECODABET[ ENCODABET.charCodeAt( i ) ] = i; */

var FACTORS = [
   1,
   85,
   7225,     // 85^2
   614125,   // 85^3
   52200625  // 85^4
];

var UTF8_Encoder = null;

function base85_decode ( input, output ) {
   if ( typeof( input ) === 'string' ) {
      if ( UTF8_Encoder === null ) UTF8_Encoder = new TextEncoder( "utf-8" );
      input = UTF8_Encoder.encode( input );
   }
   var factor = 5, sum = 0, length = 0, size = Math.floor( input.length * 0.8 );

   if ( output === undefined )
      output = new Uint8Array( size )
   else if ( output.length < size ) 
      throw "Buffer too small for Base85 decode";

   for( var i= 0, len = input.length ; i < len ; ++i ) {
      sum += DECODABET[ input[ i ] ] * FACTORS[--factor];
      if ( factor === 0 ) {
         for ( var e = 24 ; e >= 0 ; e -= 8 )
            output[ length++ ] = (sum >>> e) & 0xFF;
         sum = 0;
         factor = 5;
      }
   }
   if ( factor < 5 ) {
      sum /= FACTORS[factor];
      for ( var e = (3 - factor) * 8 ; e >= 0 ; e -= 8 )
         output[ length++ ] = (sum >>> e) & 0xFF;
   }

   if ( length === output.length )
      return output;
   return new Uint8Array( output, 0, length );
}


root.Base85 = {
   decode: base85_decode,
};

})( this || window || global );