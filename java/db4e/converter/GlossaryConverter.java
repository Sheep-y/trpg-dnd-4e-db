package db4e.converter;

import db4e.data.Category;
import db4e.data.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlossaryConverter extends Converter {

   public GlossaryConverter ( Category category ) {
      super( category );
   }

   private final Matcher regxLowercaseTitle = Pattern.compile( "(<h1[^>]*+>)([a-z])" ).matcher( "" );
   private final Matcher regxFlavor = Pattern.compile( "<p class=flavor>(?!.*<p class=flavor>)" ).matcher( "" );

   @Override protected void correctEntry () {
      if ( entry.getId().startsWith( "skill" ) ) { // Fix skills missing "improvising with" title
         if ( ! find( "IMPROVISING WITH" ) ) {
            data( regxFlavor.reset( data() ).replaceFirst( "<h3>IMPROVISING WITH "+entry.getName().toUpperCase()+"</h3><p class=flavor>" ) );
            test( TEXT, "<h3>IMPROVISING WITH " );
            fix( "content" );
         }
      }
      switch ( entry.getId() ) {
         case "glossary03": // Ability Scores
            entry.setFields( "Rules", "Rules Other", null ).setContent( unzip(
               "wO3tl<2DfdEBkgXVEgI@O^YHm;O6ddL5m_t4!CDpT3duEQY9%T`uClsK5QlSIai=S081jdoSm6nGB?G7GNVHHy)vp{zg(@g(XpY&%y|ws?~BFqrdmGgXHscIbD+w*z0R2w{Zs2~M>&Vg>ndtz$!#d}tYVGT@2D!8pfhDi*_=wY5sz7+yvibb9iq;{Z}b>F+ifhSbn7g>#abdw=>tE@tk_KH&PFyyZ`o!%rLQ{VuAHWzw}_@gsir`&(N^0nVD$*z1qxkoQh}Zjqy1Go%OT9qZY`0A?MZp2;7X3VMD|F>aDJ3p&r7@PGSt)YI(CL=^PBgTQ2$Y7g|Irx>Yzn|d=v#y3%ii5%Qbt7g^Gx%g2;e3rc!T<C~?wIyS|yzs$D7riyz5(S}aJnE3ES+02OtqEU7(vwZ#_e8msd?z#_B{t+2{AR0@IVix+2PGH>nn+%F|<F(<^M3~3d{vGTnFYckXDp3PKnXy31Bp$5{W7?4|nimPQWB2Do011x)$EZct_Xc!c$+!OmbeJOO$WYf{0Q*`H}vXM5RBc*nX@u_gMNn934eHPtJep@WHS>71E`~vb|MpYZpz%bKVVPjG;DpU*hz&jL2we%q8vTi`!39#d4fG1d1u`nzpLt&?l6cT5<ke06O5lHyski5(FM#B-Av(am)Cw_4B2|2>;ITWp}K`oGwflUg`J+6gA1TAy67)P>_DQF%|oZxL>e>CE`0{=jV2&{~hTZpRER<(t(*W!Gnm@zydHF`w&nk@<&YIs+|R{~)SFex_@U`2}%XURfiT3)Y{tW=&u4Ulp~L4h4BrvZ81OWKtdDn*izMgv=IQ+Y{69V>vEwp0fumg3D-lZ<6I27@qoPV43(-)Y~`$QfIFw0Z4Q-CVTAX<@&|8n2CY!WM_lIl<%LiNoiiBvTD>!@%Ht4nuV9cbn3i!Z{nm&wXoMD<gTHEjF;Q6>`s2+W;VubAWTM-ZJmjN(yd@ygTCx4m1vU%Qu$^KT7!w+Yc$jQ&b-xBq!;{es`M}#?$F^^WsjEx|H%I;^EJ__Bl1x??t&vQEug;@SmXe&KOOc;B7F+Xr#SRti>SisXMk0_XZpe%G>tn!I2nX>X9Wzq0VXRQKD!+pK%FszG-!RV5~~5;gYVW6MnO_CkX#PbbS-b3(|fYYOia1&7lc?2SvMYL#=17EQMRP0j;HCbo;6;_rPW|%XP?fg}%zI7+h8=7R^Z6F60?aKh!9HXPJd2NQ;V<n`02Zru28ZJGHUz)8=ndy3gVadtb5q<PqB6^}cz0_LC-fIvkzD(w@0=GRnaN(%^R0_C|TB(dqz}L)Rfu896=|x*G8|ggnFCAEw@HaN!33u2flj;nt<jRqPm(zR&SVhqs(EuX2AQX)n2cNm<qZt5UCx4h83Tc6r}F%dPP~{SN~OpMiPp+zu=$IieTWv&AoB3GeWhZoy2S<l=4VFU$@7#H;Js)vvE6I=^)>V(y=C79hTy{!k+ooUUcXHrI984`2T@yZ$*T8kX" ) );
            test( TEXT, "Strength, Constitution, Dexterity, Intelligence, Wisdom, and Charisma" );
            fix( "new entry" );
            break;

         case "glossary381": // Concordance, change to Artifact
            entry.setName( "Artifact" );
            swap( "<h1 class=player>Concordance</h1><p class=flavor>", "<h3>Concordance</h3><p>", "new entry" );
            data( unzip( "ZB$K<+cpsWD|6cx*f_|2yTBIdp@*CrAm=ePwuq%T%8;_Ef8RHxZFz$p1j{0aZ{El7W&A=RaVdYyi8ry=H>>f)p}u?`zr4Q8*Tqxfi|&hsM!pC!CAmnV<5DRr%@%{uC#s`t6gWRsItm?iIggdpC>Wor49~=Q@J-RE$~3F7ZIrn#CM3Dwr(l_DKWI=t=!U2H{Hcmf6sH-DkY;=hBOCsRJ)ux5Ct8J59S`(YXpMD5cQ)bk#OD|&0=0M-#8*D3>r5u$8@|x2RdU!!Pbev%r?a2{t4?6nRSd^CdA{p%G~xGFy_DqxBH5Cp9A1w0>d)JbzH1dw-4q&P0rbeZ{eyOclxn)kK*tU6ttm&j5fjd6>%uYdF$tYeqFA>_$_M&8lLJ1t(z|Nqi8ur)F5Ux`-<%~>$ycvTAV-Z*9M~p7FJkEDUhq4`LcYT!ZB6!=>FYOW8#JT&1Uq(6Crv*W&>~NT!N%E-3QJ<!rCCQUNX5UgxEJCO?^)OEnv|u?5}HD8!mZW(ksY}GT%tAC_H#9($JjOe`i%;2g^KR(-1hw`NM2Oilg~(LahiA#cF&n7yoL42+z||exSVVeAVH^T$uV$^7oU!6__rSEF0B)q6dTy}?M6-R@`}Em`}Wh4LsLJ+cSp&6U>|J!|7@wJ7OA>Uf+rbpu1jg-<iCSPycDIAFSmUz%)@w`l~d1sAJjY6UmV8AjR<P6IJZvSca9!RG!3J#gS4N8P>9oTbm)d2J-BVyz(+HuShoZHqmw)Y(;YzQ<$`K>;0G0vcTaCnvt_PVS=T6OXhXVU6foL-CUE*vI>YM@FFpJ0Q3F7q*ru^ecbY$NAA#MZCcoYs{UUH5@>#%tmOMBi2;fY()>xN8+6|}~(P-KL(O1J_<|LXW4`|YhZ~Z|IC^HrB&l5tisT*(~?Y1^uKXivX1@V*KcW?Bxq&_a|ZBlL+UE8x8R)4jBjb#*^-K_Cra6?Tox335K{&6!Fh9cEx^?IN7TdQ@fHn0zJ!5dQad4By5" )
                  + data() );
            test( TEXT, "Artifacts have a level but no price" );
            break;

         case "glossary664": // Reading a Weapon Entry
            entry.setName( "Weapons" );
            swap( "Reading a Weapon Entry", "Weapons", "consistency" );
            break;

         case "glossary0453": // Item Set
            entry.setFields( "Rules", "Rules Other", null ).setContent( unzip(
               "byZDo+b|6ME4;M@0=H=|8;}L)p%`-5R;=xIrfu35Dod^;C(XYfNq@vCIv@cYOC%p3A73`j8VZXse(NkADQtI%T6m(gxo+0mO}D*=7Hd<0k@t}j6y7B?E<)w|06swL1L<*u)L;UHqQQVgB0WavFr*_C=s4C|DpCdySTvfg!3Z2<31b59NMGjUs4{6tXCegJ6Iv?a(2(PVt#PL9TX21wOOc9iYUD*6fgnTbBS$1(6CZ{Qo_srGA8**`x%!@=yVC4U*`tLn_>Mv{6a_g|6@wkSDybt^GDNL)s_Z5EB5zK{)ml5-<Y6n{`voDw!qi?Be9H;#Q5#>tn7Kx+NKgmgcZ=6TaxyJtu&d7a?tF|SpAn6gXnUJXXK79lS3^&yxVDe-)B8wFvf<(U56*D<0npe{gvPYOoKS*EnNrd!AQ?ElC;rJ(2^7;z&$EX&E@P@AON!&%ocAKmnOA3RD%_l*8nUJE&2b09ppVSgxhj=}Q6U*Dr=cvU1SP!V;f?@WrmH1)lAvf<`dFZwQLt(%Qy95^FsT`0PWcweCdH+>WBfF=pp3~Mm@0jBDa6n_N+w}qSjobAeC2yR>y$4#UA5x%3QAAWUC65TTJ?aT<im4D;W9P8uNygN2$`R90?qyMmPb=k&=pdueQ;M9{PnKn*)R8uanpN2T{0Xh5^+T3plgmRFa$He^MMl%GhWWXLyZFd)49(oQO8(Vk+!RT#DOtMU9>#=41W6IhQNp;di4Sw4!U7unQ$RNGVqnEcZJ~3HrL&D)7)*p|Niyz2)pNpAMo_>%w~8eW~d%AZ;^IiQZ%)jY08-<wg=C`I0`^kBro3F?F#Z<k3^0wiHXE<_AC&?`P3|#=fUYl>l|z|YSciy#C+Jo)ui#$hzEdNmmQNKj&)Dwh1QDK9U4D5$EPc!+@B^kZ(2Al;55Bu;3#sbAVdF%ZXWGRS7IW#y!mo{d$Yd1l6N~7*@y=QunyQYnq#)I$&N$=j$Nf7J)c*xII_vS^~|Vj9?i9MzTNbf*XP!-_{*K!K2Fb~tow4$QGKAmQv8El6mC}xrRG+?^3(e6MwRjp" ) );
            test( TEXT, "A magic item set contains four or more items" );
            fix( "new entry" );
            break;
      }
      if ( find( regxLowercaseTitle ) )
         swap( regxLowercaseTitle.group(), regxLowercaseTitle.group(1) + regxLowercaseTitle.group(2).toUpperCase(), "formatting" );
      super.correctEntry();
   }

   @Override protected Set<String> getLookupName ( Entry entry, Set<String> list ) {
      switch ( entry.getId() ) {
         case "skill27": // Athletics
            return append( list, "Athletics", "Escape", "Climb", "Climbing", "Swim", "Swimming", "Jump", "Jumping", "Long Jump" );
         case "glossary03": // Ability Scores
            return append( list, "Ability Score", "Strength", "Str", "Constitution", "Con", "Dexterity", "Dex", "Intelligence", "Int", "Wisdom", "Wis", "Charisma", "Cha" );
         case "glossary86": // Teleportation
            return append( list, "Teleport", "Teleportation" );
         case "glossary159": // Hit Points
            return append( list, "HP", "Hit Point", "Bloodied" );
         case "glossary163": // Sustained Durations
            return append( list, "Sustained Durations", "Sustain", "Sustain Minor", "Sustain Move", "Sustain Standard", "Sustain No Action" );
         case "glossary179": // Defense Scores
            return append( list, "Defense Scores", "Defenses", "Defense", "Fortitude", "Reflex", "Will" );
         case "glossary341": // Dying and Death
            return append( list, "Dying", "Death", "Death Saving Throw", "Die", "Dies", "Kill", "Drop to 0 or" );
         case "glossary487": // Carrying, Lifting and Dragging
            return append( list, "Carry", "Carrying", "Lift", "Lifting", "Drag", "Dragging", "Normal Load", "Heavy Load", "Maximum Drag Load" );
         case "glossary622": // Action Types
            return append( list, "Action Type", "Standard Action", "Move Action", "Minor Action", "Immediate Reaction", "Immediate Action", "Immediate Interrupt", "Opportunity Action", "Free Action" );
         case "glossary623": // Languages
            return append( list, "Language", "Script" );
         case "glossary670": // Magic Item Level and Rarity
            return append( list, "Magic Item Level and Rarity", "Common", "Uncommon", "Rare" );
         case "glossary69": case "glossary659" : case "glossary661" : case "glossary664" : // Implement, Armor, Shields, Weapon
            if ( entry.getId().equals( "glossary69" ) ) list.add( "Implements" );
            append( list, "Proficiency", "Proficiencies" ); // Fall through to add singular lookup
      }
      String name = entry.getName();
      if ( name.endsWith( " speed" ) || name.endsWith( " Attack" ) )
         return append( list, name, name.substring( 0, name.length() - 6 ) );
      list.add( name );
      if ( name.startsWith( "Object" ) )
         list.add( "Object" );
      else if ( name.toLowerCase().contains( " size" ) )
         list.add( "size" );
      if ( name.endsWith( "s" ) ) {
         list.add( name.substring( 0, name.length() - 1 ) );
         if ( name.endsWith( "es" ) ) {
            list.add( name.substring( 0, name.length() - 2 ) );
            if ( name.endsWith( "ies" ) )
               list.add( name.substring( 0, name.length() - 3 ) + 'y' );
         }
      } else if ( name.endsWith( "ing" ) && ! name.equals( "Sling" ) )
         if ( name.equals( "Dying" ) )
            list.add( "Die" );
         else
            list.add( name.substring( 0, name.length() - 3 ) );
      return list;
   }
}