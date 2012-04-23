package org.dbpedia.extraction.server.util

import io.Source
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.mappings.ClassMapping
import org.dbpedia.extraction.util.Language
import scala.collection.mutable
import java.io.Writer

/**
 * Hold template statistics
 * TODO: comment
 * @param templateCount apparently the number of pages that use the template. a page that uses
 * the template multiple times is counted only once.
 */
class TemplateStats
{
    var templateCount = 0
    val properties = new mutable.HashMap[String, Int]
    
    override def toString = "TemplateStats[count:" + templateCount + ",properties:" + properties.mkString(",") + "]"
}

object WikipediaStatsFormat {
    val WikiStatsTag = "wikiStats|"
    val RedirectsTag = "redirects|"
    val RedirectTag = "r|" // tag is used ~10^6 times - use short tag to save space
    val TemplatesTag = "templates|"
    val TemplateTag = "template|"
    val CountTag = "count|"
    val PropertiesTag = "properties|"
    val PropertyTag = "p|" // tag is used ~10^5 times - use short tag to save space
}

import WikipediaStatsFormat._

// Hold template redirects and template statistics
class WikipediaStats(val language : Language, val redirects: mutable.Map[String, String], val templates: mutable.Map[String, TemplateStats])
{
    def checkForRedirects(mappings: Map[String, ClassMapping]) =
    {
        val templateNamespacePrefix = Namespace.Template.getName(language) + ":"
        val mappedRedirects = redirects.filterKeys(title => mappings.contains(title))
        mappedRedirects.map(_.swap)
    }
    
    def write(writer : Writer) : Unit = {
      
      writer.write(WikiStatsTag)
      writer.write(language.wikiCode)
      writer.write('\n')
      
      writer.write('\n')
      
      writer.write(RedirectsTag)
      writer.write(redirects.size.toString)
      writer.write('\n')
      for ((from, to) <- redirects) { 
        writer.write(RedirectTag)
        writer.write(from)
        writer.write('|')
        writer.write(to)
        writer.write('\n')
      }
      
      writer.write('\n')
      
      writer.write(TemplatesTag)
      writer.write(templates.size.toString)
      writer.write('\n')
      writer.write('\n')
      for ((name, stats) <- templates) {
        writer.write(TemplateTag)
        writer.write(name)
        writer.write('\n')
        writer.write(CountTag)
        writer.write(stats.templateCount.toString)
        writer.write('\n')
        writer.write(PropertiesTag)
        writer.write(stats.properties.size.toString)
        writer.write('\n')
        for ((property,count) <- stats.properties) {
          writer.write(PropertyTag)
          writer.write(property)
          writer.write('|')
          writer.write(count.toString)
          writer.write('\n')
        }
        writer.write('\n')
      }
      writer.write('\n')
    }
}

class WikipediaStatsReader(lines : Iterator[String]) extends CollectionReader(lines)
{
  def read() : WikipediaStats = {
  
    var line = ""
    
    val language = Language(readTag(WikiStatsTag))
    readEmpty
    
    val redirCount = readCount(RedirectsTag)
    val redirects = new mutable.HashMap[String, String]()
    var redirIndex = 0
    while(redirIndex < redirCount) {
      redirects += readRedirect
      redirIndex += 1
    }
    readEmpty
    
    val templateCount = readCount(TemplatesTag)
    readEmpty
    val templates = new mutable.HashMap[String, TemplateStats]()
    var templateIndex = 0
    while(templateIndex < templateCount) {
      val templateStats = new TemplateStats
      templates(readTag(TemplateTag)) = templateStats
      templateStats.templateCount = readCount(CountTag)
      val propCount = readCount(PropertiesTag)
      var propIndex = 0
      while(propIndex < propCount) {
        templateStats.properties += readProperty
        propIndex += 1
      }
      templateIndex += 1
      readEmpty
    }
    readEmpty
    
    new WikipediaStats(language, redirects, templates)
  }
  
  private def readRedirect : (String, String) = {
    val tail = readTag(RedirectTag)
    val pair = tail.split("\\|", -1)
    if (pair.length == 2) {
      return (pair(0), pair(1))
    }
    throw new Exception("expected line starting with '"+RedirectTag+"' followed by two strings, found '"+RedirectTag+tail+"'")
  }
  
  private def readProperty : (String, Int) = {
    val tail = readTag(PropertyTag)
    val pair = tail.split("\\|", -1)
    if (pair.length == 2) {
      try { return (pair(0), pair(1).toInt) }
      catch { case nfe : NumberFormatException => /* fall through, throw below */ }
    }
    throw new Exception("expected line starting with '"+PropertyTag+"' followed by two strings, found '"+PropertyTag+tail+"'")
  }
  
}