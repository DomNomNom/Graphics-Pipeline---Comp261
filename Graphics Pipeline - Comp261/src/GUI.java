import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;


public class GUI {
  final BufferedImage image;
  int mouseX, mouseY;
  boolean redraw;
  boolean running = true;
  
  JFrame myFrame;
  JComponent imageComp;
  int[] colours;
  
  public GUI(int[] colours, int width, int height) {
    this.colours = colours;
    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    myFrame = new JFrame("Render Pipeline");
    imageComp = new JComponent() {
      @Override
      protected void paintComponent(Graphics g) {
        draw(g);
      }
    };
    imageComp.setPreferredSize(new Dimension(width, height));
    myFrame.add(imageComp, BorderLayout.CENTER);
    myFrame.addMouseMotionListener(new MouseMotionListener() {
      
      @Override
      public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        redraw = true;
      }
      
      @Override
      public void mouseDragged(MouseEvent e) {}
    });
    myFrame.pack();
  }
  
  public void mainLoop() {
    myFrame.setVisible(true);
    
    //animation loop
    while(running){
      imageComp.repaint();
      try {  Thread.sleep(15);  }
      catch (InterruptedException e) { throw new Error(e); }
    }
  }
    
  static int convertToRGB(int red, int green, int blue){
    return red<<16 | green<<8 | blue;
  }
  
  void draw(Graphics g){
    image.setRGB(0, 0, image.getWidth(), image.getHeight(), colours, 0, image.getWidth());
    g.drawImage(image, 0, 0, null);
  }
}
