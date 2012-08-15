/* Code for COMP261 Assignment
 */

import java.util.*;
import java.io.File;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.*;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Code that you may choose to use if you wish. */

public class ImageSample {

  private static int imageWidth = 800;
  private static int imageHeight = 800;

  private static JFrame frame;
  private static BufferedImage image;
  private static JComponent drawing;

  public static void main(String[] args) {
    setupFrame();

    Color[][] bitmap = new Color[imageWidth][imageHeight];
    float start = 0;
    for (int t = 0; t < 1000; t++) {
      // make the bitmap
      for (int i = 0; i < imageWidth; i++) {
        for (int j = 0; j < imageHeight; j++) {
          bitmap[i][j] = Color.getHSBColor((i + j + start) / (imageWidth + imageHeight), 1.0f, 1.0f);
        }
      }
      // render the bitmap to the image
      convertToImage(bitmap);
      // draw it.
      drawing.repaint();

      start -= 5;/*
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException e) {
      }*/
    }
    saveImage("TestImage.png");

  }

  /**
   * Creates a frame with a JComponent in it. Clicking in the frame will close
   * it.
   */
  public static void setupFrame() {
    frame = new JFrame();
    frame.setSize(imageWidth + 10, imageHeight + 20);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    drawing = new JComponent() {
      protected void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, null);
      }
    };
    frame.add(drawing, BorderLayout.CENTER);
    frame.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        frame.dispose();
      }
    });
    frame.setVisible(true);
  }

  /**
   * Converts a 2D array of Colors to a BufferedImage. Assumes that bitmap is
   * indexed by column then row and has imageHeight rows and imageWidth columns.
   * Note that image.setRGB requires x (col) and y (row) are given in that
   * order.
   */
  public static BufferedImage convertToImage(Color[][] bitmap) {
    image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < imageWidth; x++) {
      for (int y = 0; y < imageHeight; y++) {
        image.setRGB(x, y, bitmap[x][y].getRGB());
      }
    }
    return image;
  }

  /**
   * writes a BufferedImage to a file of the specified name
   */
  public static void saveImage(String fname) {
    try {
      ImageIO.write(image, "png", new File(fname));
    }
    catch (IOException e) {
      System.out.println("Image saving failed: " + e);
    }
  }

}
