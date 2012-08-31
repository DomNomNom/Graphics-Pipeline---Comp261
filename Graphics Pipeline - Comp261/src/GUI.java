import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;


public class GUI implements MouseMotionListener, MouseListener, MouseWheelListener {
  public static final float rotationSpeed = 0.004f;
  
  final BufferedImage image;
  int mouseX, mouseY;
  boolean redraw;
  boolean running = true;
  
  JFrame myFrame;
  JComponent imageComp;
  int[] colours;
  private boolean justRendered = false;
  private RenderPipeline p;
  
  public GUI(RenderPipeline p) {
    this.p = p;
    colours = p.zBuffer.colours;
    image = new BufferedImage(p.width, p.height, BufferedImage.TYPE_INT_RGB);
    imageComp = new JComponent() {
      @Override
      protected void paintComponent(Graphics g) {
        draw(g);
      }
    };
    imageComp.setPreferredSize(new Dimension(p.width, p.height));
    myFrame = new JFrame("Render Pipeline");
    myFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    myFrame.setResizable(false);
    myFrame.add(imageComp, BorderLayout.CENTER);
    myFrame.addMouseListener(this);
    myFrame.addMouseMotionListener(this);
    myFrame.addMouseWheelListener(this);
    myFrame.getRootPane().registerKeyboardAction(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) { myFrame.setVisible(false); running = false; }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
    );

    // number keys control our flags
    myFrame.addKeyListener(new KeyListener() {
      public void keyTyped(KeyEvent k) {
        boolean wasValidChar = true;
        switch (k.getKeyChar()) {
          // toggle the flag corresponding to the number keys
          case '1':  Flags.colourAveraging ^= true;    break;
          case '2':  Flags.smoothSurface   ^= true;    break;
          case '3':  Flags.noSpecular      ^= true;    break;
          case '4':  Flags.noDiffuse       ^= true;    break;
          case '5':  Flags.noAmbient       ^= true;    break;
          //case '6':  Flags.colourAveraging ^= true;    break;
          //case '7':  Flags.colourAveraging ^= true;    break;
          default: wasValidChar = false;
        }
        if (wasValidChar) startRendering();
      }
      public void keyReleased(KeyEvent arg0) {}
      public void keyPressed(KeyEvent arg0)  {}
    });
    
    myFrame.pack();

    startRendering();
  }

  public void startRendering() {
    // draw unless we have drawn very recently
    if (!justRendered) {
      justRendered = true;
      p.render();
    }
  }
  
  public void mainLoop() {
    myFrame.setVisible(true);
    
    //animation loop
    while(running){
      imageComp.repaint();
      justRendered = false;
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

  
  @Override
  public void mouseDragged(MouseEvent e) {
    if (e.isControlDown()) {
      p.lightRotation.x += (e.getX() - mouseX) * -rotationSpeed;
      p.lightRotation.y += (e.getY() - mouseY) *  rotationSpeed;
    }
    else {
      p.objectRotation.x += (e.getX() - mouseX) * -rotationSpeed;
      p.objectRotation.y += (e.getY() - mouseY) *  rotationSpeed;
    }
    recordMousePos(e);
    startRendering();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    recordMousePos(e);
    p.customTranslation.x = mouseX;
    p.customTranslation.y = mouseY;
    startRendering();
  }

  public void mouseClicked(MouseEvent arg0) {}
  public void mouseEntered(MouseEvent arg0) {}
  public void mouseExited(MouseEvent arg0) {}
  public void mouseReleased(MouseEvent arg0) {}
  public void mousePressed(MouseEvent e) {
    recordMousePos(e);
  }
  
  private void recordMousePos(MouseEvent e){
    mouseX = e.getX();
    mouseY = e.getY();
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    p.scale = p.scale + p.scale*0.25f*e.getWheelRotation();
    startRendering();
  }
}
