import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;


public class GUI implements MouseMotionListener {
  final BufferedImage image;
  int mouseX, mouseY;
  boolean redraw;
  boolean running = true;
  
  JFrame myFrame;
  JComponent imageComp;
  int[] colours;
  private boolean currentlyRendering = false;
  private RenderPipeline p;
  
  public GUI(RenderPipeline p) {
    this.p = p;
    colours = p.zBuffer.colours;
    image = new BufferedImage(p.width, p.height, BufferedImage.TYPE_INT_RGB);
    myFrame = new JFrame("Render Pipeline");
    imageComp = new JComponent() {
      @Override
      protected void paintComponent(Graphics g) {
        draw(g);
      }
    };
    imageComp.setPreferredSize(new Dimension(p.width, p.height));
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
    startRendering();
    myFrame.addMouseMotionListener(this);
  }

  public synchronized void startRendering() {
    if (!currentlyRendering) {
      currentlyRendering = true;
      p.render();
      currentlyRendering = false;
    }
  }
  
  public void mainLoop() {
    myFrame.setVisible(true);
    
    //animation loop
    while(running){
      imageComp.repaint();
      try {  Thread.sleep(30);  }
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

  
  @Override
  public void mouseDragged(MouseEvent e) {
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    p.customTranslation = new PVector(e.getPoint().x, e.getPoint().y);
    startRendering();
  }
}
