package es.valledecernego.game;

import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.content.Context;
import java.nio.*;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {
    GameRenderer renderer;
    TextView status;
    int wood=0, stone=0, seeds=4, harvest=0;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512|4096);
        FrameLayout root=new FrameLayout(this);
        GLSurfaceView gl=new GLSurfaceView(this);
        gl.setEGLContextClientVersion(2);
        renderer=new GameRenderer();
        gl.setRenderer(renderer);
        gl.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(gl);
        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(24,14,24,8);
        top.setBackgroundColor(0x55301f16);
        status=new TextView(this);
        status.setTextColor(Color.WHITE); status.setTextSize(15);
        status.setText("VALLE DE CERNEGO   •   Día 1   ☀   Madera 0   Piedra 0   Cosecha 0");
        top.addView(status);
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        root.addView(top,tp);
        Hud hud=new Hud(this);
        root.addView(hud,new FrameLayout.LayoutParams(-1,-1));
        hud.setOnAction(new Runnable(){ public void run(){ action(); }});
        hud.setOnJump(new Runnable(){public void run(){renderer.jump();}});
        setContentView(root);
    }
    void action(){
        float d=renderer.nearby();
        if(d<2.4f){
            int k=renderer.nearbyKind();
            if(k==1){wood+=2;renderer.removeNearby();toast("Has recogido madera +2");}
            else if(k==2){stone+=2;renderer.removeNearby();toast("Has recogido piedra +2");}
            else if(k==3){if(seeds>0){seeds--;harvest+=3;renderer.harvest();toast("¡Cosecha recogida! +3");}else toast("No quedan semillas");}
            else if(k==4){toast("La vaca está tranquila. Pronto podrás darle alimento.");}
            else {toast("No hay nada con lo que interactuar");}
        } else toast("Acércate a un árbol, piedra, cultivo o animal");
        status.setText("VALLE DE CERNEGO   •   Madera "+wood+"   Piedra "+stone+"   Semillas "+seeds+"   Cosecha "+harvest);
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    class Hud extends View {
        Paint p=new Paint(3); float jx=0,jy=0,baseX=0,baseY=0; boolean joy=false; Runnable action,jump;
        Hud(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setOnAction(Runnable r){action=r;} void setOnJump(Runnable r){jump=r;}
        protected void onDraw(Canvas c){
            super.onDraw(c); float w=getWidth(),h=getHeight();
            p.setColor(0x665c5043); c.drawCircle(105,h-115,68,p);
            p.setColor(0x99ffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(105,h-115,68,p);p.setStyle(Paint.Style.FILL);
            c.drawCircle(105+jx*38,h-115+jy*38,27,p);
            button(c,w-95,h-100,48,"HACER",0xff6d8c55);
            button(c,w-205,h-75,34,"SALTAR",0xff6b7e9b);
            p.setColor(Color.WHITE);p.setTextSize(17);c.drawText("Mueve el joystick • Arrastra a la derecha para mirar",w/2-190,h-25,p);
        }
        void button(Canvas c,float x,float y,float r,String s,int color){
            p.setColor(color);c.drawCircle(x,y,r,p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(s.length()>5?11:13);c.drawText(s,x,y+4,p);p.setTextAlign(Paint.Align.LEFT);
        }
        public boolean onTouchEvent(android.view.MotionEvent e){
            float x=e.getX(),y=e.getY(),w=getWidth(),h=getHeight();
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                if(x<230&&y>h-240){joy=true;baseX=105;baseY=h-115;updateJoy(x,y);return true;}
                if(x>w-155&&y>h-175){if(action!=null)action.run();return true;}
                if(x>w-260&&y>h-140){if(jump!=null)jump.run();return true;}
                renderer.startLook(x,y);return true;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE){
                if(joy){updateJoy(x,y);}else renderer.look(x,y);return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){joy=false;jx=jy=0;renderer.setMove(0,0);invalidate();return true;}
            return true;
        }
        void updateJoy(float x,float y){jx=Math.max(-1,Math.min(1,(x-baseX)/58));jy=Math.max(-1,Math.min(1,(y-baseY)/58));renderer.setMove(jx,jy);invalidate();}
    }

    static class GameRenderer implements GLSurfaceView.Renderer {
        FloatBuffer vb,cb; int program,pos,color,mvp; float[] proj=new float[16],view=new float[16],model=new float[16],mvpM=new float[16];
        volatile float moveX,moveY,px=0,pz=5,yaw=0,pitch=0; float vy=0; boolean jumping=false;
        ArrayList<float[]> objects=new ArrayList<>(); // x,z,type,active
        GameRenderer(){
            for(int i=0;i<9;i++){objects.add(new float[]{-7+i*1.7f,-3-(i%3)*2,1,1});}
            objects.add(new float[]{5,-4,2,1}); objects.add(new float[]{-3,-7,2,1});
            for(int i=0;i<4;i++)objects.add(new float[]{-4+i*2.4f,1,3,1});
            objects.add(new float[]{3,-1,4,1});
        }
        public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            GLES20.glClearColor(.56f,.72f,.83f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            String vs="attribute vec3 aPos; uniform mat4 uMVP; void main(){gl_Position=uMVP*vec4(aPos,1.0);}";
            String fs="precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}";
            int v=shader(GLES20.GL_VERTEX_SHADER,vs),f=shader(GLES20.GL_FRAGMENT_SHADER,fs);
            program=GLES20.glCreateProgram();GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f);GLES20.glLinkProgram(program);
            pos=GLES20.glGetAttribLocation(program,"aPos");color=GLES20.glGetUniformLocation(program,"uColor");mvp=GLES20.glGetUniformLocation(program,"uMVP");
            float[] cube={-0.5f,-0.5f,-0.5f, .5f,-.5f,-.5f, .5f,.5f,-.5f, -.5f,-.5f,-.5f, .5f,.5f,-.5f, -.5f,.5f,-.5f,
            -.5f,-.5f,.5f, .5f,.5f,.5f, .5f,-.5f,.5f, -.5f,-.5f,.5f, -.5f,.5f,.5f, .5f,.5f,.5f,
            -.5f,-.5f,-.5f, -.5f,.5f,-.5f, -.5f,.5f,.5f, -.5f,-.5f,-.5f, -.5f,.5f,.5f, -.5f,-.5f,.5f,
            .5f,-.5f,-.5f, .5f,.5f,.5f, .5f,.5f,-.5f, .5f,-.5f,-.5f, .5f,-.5f,.5f, .5f,.5f,.5f,
            -.5f,.5f,-.5f, .5f,.5f,-.5f, .5f,.5f,.5f, -.5f,.5f,-.5f, .5f,.5f,.5f, -.5f,.5f,.5f,
            -.5f,-.5f,-.5f, .5f,-.5f,.5f, .5f,-.5f,-.5f, -.5f,-.5f,-.5f, -.5f,-.5f,.5f, .5f,-.5f,.5f};
            vb=ByteBuffer.allocateDirect(cube.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();vb.put(cube).position(0);
        }
        int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
        public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,62,(float)w/h,.1f,120f);}
        void setMove(float x,float y){moveX=x;moveY=y;}
        void startLook(float x,float y){lastX=x;lastY=y;}
        float lastX,lastY;
        void look(float x,float y){yaw+=(x-lastX)*.006f;pitch=Math.max(-.35f,Math.min(.45f,pitch+(y-lastY)*.002f));lastX=x;lastY=y;}
        void jump(){if(!jumping){vy=.13f;jumping=true;}}
        public void onDrawFrame(GL10 gl){
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            float speed=.075f; px+=(float)(Math.sin(yaw)*(-moveY)+Math.cos(yaw)*moveX)*speed; pz+=(float)(Math.cos(yaw)*(-moveY)-Math.sin(yaw)*moveX)*speed;
            if(jumping){py+=vy;vy-=.008f;if(py<=0){py=0;jumping=false;}}
            float cx=px-(float)Math.sin(yaw)*6, cz=pz+(float)Math.cos(yaw)*6;
            Matrix.setLookAtM(view,0,cx,2.8f+py,cz,px,1.1f+py,pz,0,1,0);
            draw(0,-.35f,0,45,.5f,45,.25f,.42f,.23f);
            // River ribbon and dirt path
            draw(-10,-.12f,-4,2,.12f,32,.18f,.48f,.62f);
            draw(0,-.08f,6,2,.12f,28,.55f,.42f,.25f);
            // Hills in distance
            for(int i=0;i<8;i++)draw(-23+i*7,-1,-24,8,5,5,.25f,.43f,.28f);
            // trees, stones, crop plots, cow and farmhouse
            for(float[] o:objects){if(o[3]==0)continue;int t=(int)o[2];
                if(t==1){draw(o[0],.55f,o[1],.35f,1.5f,.35f,.34f,.22f,.12f);draw(o[0],1.8f,o[1],1.8f,2,1.8f,.12f,.36f,.16f);}
                if(t==2)draw(o[0],.35f,o[1],1.1f,.7f,1,.48f,.48f,.46f);
                if(t==3){draw(o[0],.1f,o[1],2,.18f,2,.32f,.22f,.10f);for(int j=0;j<3;j++)draw(o[0]-.55f+j*.55f,.5f,o[1],.16f,.65f,.16f,.16f,.48f,.12f);}
                if(t==4){draw(o[0],.55f,o[1],1.2f,.8f,1.8f,.83f,.82f,.72f);draw(o[0]+.45f,.9f,o[1]-.2f,.55f,.6f,.55f,.82f,.82f,.72f);}
            }
            // House
            draw(7,.7f,-7,3.5f,1.5f,3,.56f,.43f,.29f);draw(7,1.75f,-7,4,.7f,3.4f,.35f,.18f,.14f);
            // player
            draw(px,.9f+py,pz,.55f,1.5f,.45f,.18f,.28f,.72f);
        }
        float py=0;
        void draw(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b){
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);
            float[] mv=new float[16];Matrix.multiplyMM(mv,0,view,0,model,0);Matrix.multiplyMM(mvpM,0,proj,0,mv,0);
            GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(mvp,1,false,mvpM,0);GLES20.glUniform4f(color,r,g,b,1);
            GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,vb);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36);GLES20.glDisableVertexAttribArray(pos);
        }
        float nearby(){float best=999;for(float[] o:objects)if(o[3]>0){float d=(float)Math.hypot(px-o[0],pz-o[1]);if(d<best)best=d;}return best;}
        int nearbyKind(){float best=999;int kind=0;for(float[] o:objects)if(o[3]>0){float d=(float)Math.hypot(px-o[0],pz-o[1]);if(d<best){best=d;kind=(int)o[2];}}return kind;}
        void removeNearby(){float best=999;int ix=-1;for(int i=0;i<objects.size();i++){float[] o=objects.get(i);if(o[3]>0){float d=(float)Math.hypot(px-o[0],pz-o[1]);if(d<best){best=d;ix=i;}}}if(ix>=0&&best<2.4f)objects.get(ix)[3]=0;}
        void harvest(){for(float[] o:objects)if((int)o[2]==3&&o[3]>0){o[3]=0;break;}}
    }
}
