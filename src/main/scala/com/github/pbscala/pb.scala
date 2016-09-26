package com.github.pbscala

import com.github.nscala_time.time.Imports._
import com.github.pbscala.Units._
import jline.TerminalFactory

/**
  * We're using Output as a trait of ProgressBar, so be able
  * to mock the tty in the tests(i.e: override `print(...)`)
  */
trait Output {
  def printit(s: String) = print(s)
}

object ProgressBar {
  private val Format = "[=>-]"

  def kbFmt(n: Double): String = {
    var kb = 1024
    n match {
      case x if x >= Math.pow(kb, 4) => "%.2f TB".format(x / Math.pow(kb, 4))
      case x if x >= Math.pow(kb, 3) => "%.2f GB".format(x / Math.pow(kb, 3))
      case x if x >= Math.pow(kb, 2) => "%.2f MB".format(x / Math.pow(kb, 2))
      case x if x >= kb => "%.2f KB".format(x / kb)
      case _ => "%.0f B".format(n)
    }
  }
}

/**
  * By calling new ProgressBar with Int as a total, you'll
  * create a new ProgressBar with default configuration.
  */
class ProgressBar(_total: Int) extends Output {
  val total = _total
  var current = 0
  var isFinish = false
  var showBar, showSpeed, showPercent, showCounter, showTimeLeft = true
  private var startTime = DateTime.now
  private var units = Units.Default
  private var barStart, barCurrent, barCurrentN, barRemain, barEnd = ""

  format(ProgressBar.Format)

  /**
    * Add value using += operator without drawing.
    */
  def +=(i: Int): Int = {
    current += i
    current
  }

  /**
    * Set Units size
    * the default is simple numbers, but you can use Bytes type instead.
    */
  def setUnits(u: Units) = units = u

  /**
    * Set custom format to the drawing bar, default is `[=>-]`
    */
  def format(fmt: String) {
    if (fmt.length >= 5) {
      val v = fmt.split("").toList
      barStart = v.head
      barCurrent = v(1)
      barCurrentN = v(2)
      barRemain = v(3)
      barEnd = v(4)
    }
  }

  /**
    * Calling finish manually will set current to total and draw
    * the last time
    */
  def finish() {
    if (current < total) add(total - current)
    println()
    isFinish = true
  }

  /**
    * Add to current value and draw.
    *
    * @param          i the number to add to current value
    * @return current value
    */
  def add(i: Int): ProgressBar = {
    current += i
    draw()
    this
  }

  def draw(): ProgressBar = {
    val uncheckedWidth = TerminalFactory.get().getWidth
    val width = uncheckedWidth < 80 match {
      case true => 120
      case false => uncheckedWidth
    }

    var prefix, base, suffix = ""
    // percent box
    if (showPercent) {
      var percent = current.toFloat / (total.toFloat / 100)
      suffix += " %.2f %% ".format(percent)
    }
    // speed box
    if (showSpeed) {
      val fromStart = (startTime to DateTime.now).millis.toFloat
      val speed = current / (fromStart / 1.seconds.millis)
      suffix += (units match {
        case Default => "%.0f/s ".format(speed)
        case Bytes => "%s/s ".format(ProgressBar.kbFmt(speed))
      })
    }
    // time left box
    if (showTimeLeft) {
      val fromStart = (startTime to DateTime.now).millis.toFloat
      val left = (fromStart / current) * (total - current)
      val dur = Duration.millis(Math.ceil(left).toLong)
      if (dur.seconds > 0) {
        if (dur.seconds < 1.minutes.seconds)
          suffix += "%ds".format(dur.seconds)
        else suffix += "%dm".format(dur.minutes)
      }
    }
    // counter box
    if (showCounter) {
      prefix += (units match {
        case Default => "%d / %d ".format(current, total)
        case Bytes =>
          "%s / %s ".format(ProgressBar.kbFmt(current),
            ProgressBar.kbFmt(total))
      })
    }
    // bar box
    if (showBar) {
      val size = width - (prefix + suffix).length - 3
      if (size > 0) {
        val curCount = Math.ceil((current.toFloat / total) * size).toInt
        val remCount = size - curCount
        base = barStart
        if (remCount > 0) {
          base += barCurrent * (curCount - 1) + barCurrentN
        } else {
          base += barCurrent * curCount
        }
        base += barRemain * remCount + barEnd
      }
    }
    // out
    var out = prefix + base + suffix
    if (out.length < width) {
      out += " " * (width - out.length)
    }
    val msg = "\r" + out
    // print
    printit(msg)
    this
  }
}
