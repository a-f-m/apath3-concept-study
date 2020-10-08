package org.afm.apath3.struct

import org.afm.apath3.core.{Apath, Config, Context}

abstract class Holder(config: Config, var root: Option[AnyRef] = None) extends Apath(config) {


  private def parse(s: String): AnyRef = {

    config.acc.parse(s)
  }

  def get[R](expr: String): R = super.get[R](root.get, expr)
  def doMatch(expr: String): Iterable[Context] = super.doMatch(root.get, expr)

  def setObject[R](root: AnyRef): R = {

    this.root = Some(root)
    if (!config.acc.isPropertyMapFunc.get.apply(root) && !config.acc.isArrayFunc.get.apply(root)) //
      throw new RuntimeException(s"mismatch between accessor class and '${
        root.getClass
      }'")
    this.asInstanceOf[R]
  }

  def setObject[R](s: String): R = setObject[R](parse(s))
}

