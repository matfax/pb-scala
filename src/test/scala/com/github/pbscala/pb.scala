package com.github.pbscala

import org.scalatest._

trait MockOutput extends Output {
  var messages: Seq[String] = Seq()

  override def printit(s: String) {
    messages = messages :+ s
  }
}

class ProgressBarTest extends WordSpec with Matchers {

  "A ProgressBar" when {
    "Call add()" should {
      "increment `current` in n" in {
        var pb = new ProgressBar(100) with MockOutput
        pb.add(10)
        pb.current should be (10)
        pb.messages.last should (
          startWith ("\r10 / 100") and
            endWith regex "10.00 % (\\d+)/s (\\d+s *)* ".r)
      }
    }
    "Using += operator" should {
      "increment `current` in n" in {
        var pb = new ProgressBar(100) with MockOutput
        pb+= 10
        pb.draw()
        pb.current should be (10)
      }
    }
    "Call finish" should {
      "set `current` to `total`" in {
        var pb = new ProgressBar(1) with MockOutput
        pb.finish()
        pb.current should be (pb.total)
        pb.isFinish should be (true)
        pb.messages.size should be(2)
      }
    }
    "Set format" should {
      var pb = new ProgressBar(10) with MockOutput
      "not except string with len less than 5" in {
        pb.format("halo")
        pb.add(1)
        pb.messages.last should include regex "\\[=+>-+\\]".r
      }
      "replace bar box format" in {
        pb.format("[-> ]")
        pb += 1
        pb.draw()
        pb.messages.last should include regex "\\[-+> +\\]".r
      }
    }
  }
}
