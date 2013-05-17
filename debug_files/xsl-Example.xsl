<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
   <xsl:param name="sortOrder" select="'ascending'"/>
   <xsl:param name="sortKey1" select="'Name'"/>
   <xsl:param name="sortKey2" select="'Category'"/>
   <xsl:param name="dataType1" select="'text'"/>
   <xsl:param name="dataType2" select="'text'"/>
   <xsl:template match="/">
      <table class="resultsTable">
      <tr>
         <td>
            <a>Name</a>
         </td>
         <td>
            <a>Type</a>
         </td>
         <td>
            <a>Sourcebook</a>
         </td>
      </tr>
      <tr>
         <td colspan="3"><div>
            <table>
               <xsl:for-each select="/Data/Results/Example">
                  <xsl:sort select="*[name()=$sortKey1]" order="{$sortOrder}" data-type="{$dataType1}"/>
                  <xsl:sort select="*[name()=$sortKey2]" order="ascending" data-type="{$dataType2}"/>
                  <tr>
                     <td>
                        <a>
                           <xsl:attribute name="href">
                              <xsl:if test="ID='exampleId001'">Example-exampleId001.html</xsl:if>
                              <xsl:if test="ID='exampleId002'">Example-exampleId002.html</xsl:if>
                           </xsl:attribute>
                           <xsl:apply-templates select="Name"/>
                        </a>
                     </td>
                     <td>
                        <xsl:apply-templates select="Type"/>
                     </td>
                     <td>
                        <xsl:choose>
                           <xsl:when test="contains(SourceBook, ', ')">Multiple Sources</xsl:when>
                           <xsl:otherwise><xsl:apply-templates select="SourceBook"/></xsl:otherwise>
                        </xsl:choose>
                     </td>
                  </tr>
               </xsl:for-each>
            </table>
         </div></td>
      </tr>
      <tr>
         <td colspan="3"></td>
      </tr>
      </table>
   </xsl:template>
</xsl:stylesheet>