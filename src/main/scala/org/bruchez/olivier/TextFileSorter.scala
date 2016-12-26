package org.bruchez.olivier

import java.io._

import scala.annotation.tailrec
import scala.io._
import scala.util._

object TextFileSorter {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      println("Usage: TextFileSorter <text file>")
      System.exit(-1)
    }

    ItemsToSort(new File(args.head)).sort().get
  }
}

case class LineNumber(lineNumber: Int)

case class ItemToSort(lineNumber: LineNumber, value: String)

case class ItemsToSort(itemsFile: File, items: Seq[ItemToSort]) {
  def sort(): Try[Unit] = {
    // Make sure a sequence is always shuffled the same way
    Random.setSeed(6029271)

    val sorted = Random.shuffle(items).sortWith(lowerThan)

    val outputFile = outputFileFromItemsFile(itemsFile)

    FileHelper.writeStrings(outputFile, sorted.map(_.value + "\n"))
  }

  protected def lowerThan(first: ItemToSort, second: ItemToSort): Boolean =
    Triplets.lowerThan(itemsFile, first, second)

  protected def outputFileFromItemsFile(itemsFile: File): File =
    new File(itemsFile.getParentFile, s"${itemsFile.getName}.sorted.txt")
}

object ItemsToSort {
  def apply(itemsFile: File): ItemsToSort = {
    val itemsToSort =
      for {
        (line, index) ← Source.fromFile(itemsFile).getLines.toList.map(_.trim).filter(_.nonEmpty).zipWithIndex
      } yield ItemToSort(LineNumber(index), line)

    ItemsToSort(itemsFile, itemsToSort)
  }
}

case class Triplet(first: LineNumber, second: LineNumber, lowerThan: Boolean) {
  def asString: String = s"${first.lineNumber},${second.lineNumber},${if (lowerThan) 1 else 0}"
}

case class Triplets(triplets: Seq[Triplet]) {
  def lowerThan(first: ItemToSort, second: ItemToSort): Option[Boolean] =
    triplets.find(t ⇒ t.first == first.lineNumber && t.second == second.lineNumber).map(_.lowerThan)

  def withTriplet(triplet: Triplet): Triplets =
    Triplets(triplets :+ triplet)

  def persist(tripletsFile: File): Try[Unit] =
    FileHelper.writeStrings(tripletsFile, triplets.map(_.asString + "\n"))
}

object Triplets {
  def apply(tripletsFile: File): Triplets = {
    val triplets =
      for {
        line ← Source.fromFile(tripletsFile).getLines.toList.map(_.trim).filter(_.nonEmpty)
        parts = line.split(",")
        firstInt = parts(0).toInt
        secondInt = parts(1).toInt
        lowerThanInt = parts(2).toInt
      } yield Triplet(LineNumber(firstInt), LineNumber(secondInt), lowerThanInt != 0)

    Triplets(triplets)
  }

  def lowerThan(itemsFile: File, first: ItemToSort, second: ItemToSort): Boolean = synchronized {
    val tripletsFile = tripletsFileFromItemsFile(itemsFile)
    val triplets = try {
      Triplets(tripletsFile)
    } catch {
      case _: FileNotFoundException ⇒
        Triplets(Seq())
    }

    triplets.lowerThan(first, second) match {
      case Some(lowerThan) ⇒
        lowerThan
      case None ⇒
        // Assumption: no line is equal => not "lower than" = "greater than"
        triplets.lowerThan(second, first) match {
          case Some(invertedLowerThan) ⇒
            !invertedLowerThan
          case None ⇒
            val lowerThan = lowerThanFromStdin(first, second)
            val triplet = Triplet(first.lineNumber, second.lineNumber, lowerThan)

            val newTriplets = triplets.withTriplet(triplet)
            newTriplets.persist(tripletsFile).get

            lowerThan
        }
    }
  }

  protected def lowerThanFromStdin(first: ItemToSort, second: ItemToSort): Boolean = {
    println("Which is lower?")
    println(s" 1) ${first.value}")
    println(s" 2) ${second.value}")

    @tailrec
    def lowerThan(): Boolean = {
      print("-> ")
      val line = StdIn.readLine()
      Try(line.trim.toInt).filter(Set(1, 2).contains) match {
        case Success(oneOrTwoInt) ⇒
          oneOrTwoInt == 1
        case _ ⇒
          println("Please enter 1 or 2.")
          lowerThan()
      }
    }

    lowerThan()
  }

  protected def tripletsFileFromItemsFile(itemsFile: File): File =
    new File(itemsFile.getParentFile, s"${itemsFile.getName}.triplets")
}

object FileHelper {
  def writeStrings(file: File, strings: Seq[String]): Try[Unit] = Try {
    val printWriter = new PrintWriter(file)

    try {
      strings.foreach(printWriter.print)
    } finally {
      printWriter.close()
    }
  }
}
