package clashcode.video.swing

import java.awt.Graphics2D
import scala.swing.MainFrame
import java.awt.Dimension
import scala.swing.Panel
import java.awt.Color
import clashcode.video._
import javax.imageio.ImageIO
import sun.java2d.pipe.BufferedBufImgOps
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import scala.util.Random

trait AwtGraphics extends Graphics {

  def graphics: Graphics2D

}

abstract class AwtRectGraphics(widthHeightRatio: Double, border: Int, topBorder: Int) extends AwtGraphics {

  val _drawArea = drawArea

  def clear: Unit = {
    graphics.setColor(Color.WHITE)
    val x = _drawArea.offset.x
    val y = _drawArea.offset.y
    val w = _drawArea.area.w
    val h = _drawArea.area.h
    graphics.fillRect(x, y, w, h)
  }
  def paintField(max: Max) = {
    graphics.setColor(Color.BLACK)
    val field = EffectiveField.calc(_drawArea, widthHeightRatio, border, topBorder)
    (0 to (max.x / 2) - 1).foreach(i => {
      val fw = field.area.w / (max.x / 2)
      val d = i * fw;
      graphics.drawLine(field.offset.x + d, field.offset.y, field.offset.x + d, field.offset.y + field.area.h)
    })
    (0 to (max.y / 2) - 1).foreach(i => {
      val fh = field.area.h / (max.y / 2)
      val d = i * fh;
      graphics.drawLine(field.offset.x, field.offset.y + d, field.offset.x + field.area.w, field.offset.y + d)
    })
    graphics.drawRect(field.offset.x, field.offset.y, field.area.w, field.area.h)
  }
}

abstract class AwtRectGraphicsSimple(widthHeightRatio: Double, border: Int, topBorder: Int)
  extends AwtRectGraphics(widthHeightRatio, border, topBorder) {

  def paintCan(pos: Pos, max: Max) = {
    graphics.setColor(Color.RED)
    val f = EffectiveField.calc(_drawArea, widthHeightRatio, border, topBorder)
    val o: Pos = EffectiveOffset.calc(pos, max, f)
    val fw = f.area.w / (max.x / 2)
    val w = (fw.toDouble / 10).toInt
    graphics.fillRect(o.x - w, o.y - w, 2 * w, 2 * w)
  }
  def paintRobot(pos: Pos, dir: Direction, max: Max) = {
    graphics.setColor(Color.GREEN)
    val f = EffectiveField.calc(_drawArea, widthHeightRatio, border, topBorder)
    val o: Pos = EffectiveOffset.calc(pos, max, f)
    graphics.fillRect(o.x - 5, o.y - 5, 10, 10)
  }

}

abstract class AwtRectGraphicsImages(widthHeightRatio: Double, border: Int, topBorder: Int)
  extends AwtRectGraphics(widthHeightRatio, border, topBorder) {

  override def paintField(max: Max) = {
    graphics.setColor(Color.BLACK)
    val field = EffectiveField.calc(_drawArea, widthHeightRatio, border, topBorder)
    val fw = field.area.w.toDouble / max.x
    val fh = field.area.h.toDouble / max.y
    var k = 0;
    for (i <- (0 until (max.x / 2))) {
      for (j <- (0 until (max.y / 2))) {
        val img = ImageProvider.kacheln(k % ImageProvider.kacheln.size)
        val iw = img.getWidth()
        val ih = img.getHeight()
        val sx = 2 * fw / iw
        val sy = 2 * fh / ih
        val transform = AffineTransform.getTranslateInstance(field.offset.x + 2 * i * fw, field.offset.y + 2 * j * fh)
        transform.concatenate(AffineTransform.getScaleInstance(sx, sy))
        graphics.drawImage(img, transform, null)
        k += 1
      }
    }
  }

  def paintCan(pos: Pos, max: Max) = {
    graphics.setColor(Color.RED)
    val f = EffectiveField.calc(_drawArea, widthHeightRatio, border, topBorder)
    val o: Pos = EffectiveOffset.calc(pos, max, f)
    val fw = f.area.w
    val s = fw.toDouble / 3000
    val transform = AffineTransform.getTranslateInstance(o.x, o.y)
    transform.concatenate(AffineTransform.getScaleInstance(s, s))
    graphics.drawImage(ImageProvider.can, transform, null)
  }

  def paintRobot(pos: Pos, dir: Direction, max: Max) = {
    graphics.setColor(Color.GREEN)
    val f = EffectiveField.calc(_drawArea, widthHeightRatio, border, topBorder)
    val o: Pos = EffectiveOffset.calc(pos, max, f)
    val fw = f.area.w
    val s = fw.toDouble / 3000
    val transform = AffineTransform.getTranslateInstance(o.x, o.y)
    transform.concatenate(AffineTransform.getScaleInstance(s, s))
    graphics.drawImage(ImageProvider.robots(dir), transform, null)
  }

}

case class SwingDevice(g: Graphics2D => AwtGraphics) extends Device {

  var _stage: Option[Stage] = None
  var _f: Option[PaintFunc] = None

  val panel = new Panel {

    override def paint(awtg: Graphics2D): Unit = {
      _stage match {
        case Some(s) => _f match {
          case Some(f) => {
            f(g(awtg), s)
          }
          case None => // Nothing to be done
        }
        case None => // Nothing to be done
      }
    }

  }

  val mf = new MainFrame()
  mf.contents = panel
  mf.size = new Dimension(500, 500)
  mf.centerOnScreen
  mf.visible = true;

  def paintStage(stage: Stage, f: PaintFunc) = {
    _stage = Some(stage)
    _f = Some(f)
    panel.repaint
  }

  def drawArea: DrawArea = {
    DrawArea(Pos(0, 0), Rec(panel.size.width, panel.size.height))
  }

}

object ImageProvider {

  private def img(resName: String): BufferedImage = {
    javax.imageio.ImageIO.read(this.getClass().getClassLoader().getResourceAsStream(resName))
  }

  lazy val kacheln: List[BufferedImage] = {
    val imgNames = List(
      "img/kacheln/k01.png",
      "img/kacheln/k02.png",
      "img/kacheln/k03.png",
      "img/kacheln/k04.png",
      "img/kacheln/k05.png",
      "img/kacheln/k06.png")
    val images = imgNames.map(name => img(name))
    val x = for (i <- 1 to 500) yield {
      images(Random.nextInt(images.size))
    }
    x.toList
  }

  lazy val robots: Map[Direction, BufferedImage] = {
    val imgNames = List(
      (S, "img/robots/r00.png"),
      (SE, "img/robots/r01.png"),
      (E, "img/robots/r02.png"),
      (NE, "img/robots/r03.png"),
      (N, "img/robots/r04.png"),
      (NW, "img/robots/r05.png"),
      (W, "img/robots/r06.png"),
      (SW, "img/robots/r07.png"))
    imgNames.map { case (key, name) => (key, img(name)) }.toMap
  }
  lazy val can: BufferedImage = {
    img("img/cans/can.png")
  }
}
