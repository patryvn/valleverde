package es.valledecernego.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import java.nio.*;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {
    GameRenderer renderer;
    GLSurfaceView surface;
    TextView status;
    SharedPreferences preferences;
    int wood=0, stone=0;

    private final Handler hudHandler = new Handler(Looper.getMainLooper());
    private final Runnable hudTicker = new Runnable() {
        @Override public void run() {
            if (status != null && renderer != null) status.setText(statusText());
            hudHandler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512|4096);
        preferences = getSharedPreferences("valle_de_cernego_save", MODE_PRIVATE);
        wood = preferences.getInt("wood", 0);
        stone = preferences.getInt("stone", 0);

        FrameLayout root=new FrameLayout(this);
        surface=new GLSurfaceView(this);
        surface.setEGLContextClientVersion(2);
        renderer=new GameRenderer();
        renderer.loadFarm(preferences.getString("farm", ""));
        renderer.loadAnimals(preferences.getString("animals", ""));
        renderer.loadBuildings(preferences.getString("buildings", ""));
        renderer.loadMarket(preferences.getString("market", ""));
        renderer.loadVillagers(preferences.getString("villagers", ""));
        renderer.loadVillagerWorld(preferences.getString("villager_world", ""));
        renderer.loadSeason(preferences.getString("season", ""));
        surface.setRenderer(renderer);
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(surface);

        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(24,14,24,8);
        top.setBackgroundColor(0x66301f16);
        status=new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setText(statusText());
        top.addView(status);
        FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        root.addView(top,tp);

        Hud hud=new Hud(this);
        root.addView(hud,new FrameLayout.LayoutParams(-1,-1));
        hud.setOnAction(new Runnable(){ public void run(){ action(); }});
        hud.setOnJump(new Runnable(){ public void run(){renderer.jump();}});
        setContentView(root);
        hudHandler.post(hudTicker);
    }

    String statusText() {
        return "VALLE DE CERNEGO  •  " + renderer.getSeasonSummary() + "  •  Madera " + wood + "  Piedra " + stone + "  •  Monedas " + renderer.getCoins() + "  •  " + renderer.getFarmSummary() + "  •  " + renderer.getAnimalSummary() + "  •  " + renderer.getBuildingSummary() + "  •  " + renderer.getVillagerSummary();
    }

    void saveProgress() {
        if (preferences == null || renderer == null) return;
        preferences.edit()
            .putInt("wood", wood)
            .putInt("stone", stone)
            .putString("farm", renderer.saveFarm())
            .putString("animals", renderer.saveAnimals())
            .putString("buildings", renderer.saveBuildings())
            .putString("market", renderer.saveMarket())
            .putString("villagers", renderer.saveVillagers())
            .putString("villager_world", renderer.saveVillagerWorld())
            .putString("season", renderer.saveSeason())
            .apply();
    }

    @Override protected void onPause() {
        saveProgress();
        if (surface != null) surface.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (surface != null) surface.onResume();
    }

    @Override protected void onDestroy() {
        hudHandler.removeCallbacks(hudTicker);
        saveProgress();
        super.onDestroy();
    }

    void action(){
        float cropDistance=renderer.cropDistance();
        float objectDistance=renderer.nearby();
        if(cropDistance<2.4f && cropDistance<=objectDistance){
            int result=renderer.interactCrop();
            if(result==CropSystem.PLANTED) toast("Parcela sembrada. Pulsa HACER otra vez para regarla.");
            else if(result==CropSystem.WATERED) toast("Parcela regada. El cultivo crecerá con el tiempo.");
            else if(result==CropSystem.GROWING) toast("El cultivo todavía está creciendo.");
            else if(result==CropSystem.HARVESTED) toast("¡Cosecha recogida! +3 productos y +1 semilla.");
            else if(result==CropSystem.OUT_OF_SEEDS) toast("No tienes semillas suficientes.");
            else toast("Acércate a una parcela de cultivo.");
        } else if(objectDistance<2.4f){
            int k=renderer.nearbyKind();
            if(k==1){wood+=2;renderer.removeNearby();toast("Has recogido madera +2");}
            else if(k==2){stone+=2;renderer.removeNearby();toast("Has recogido piedra +2");}
            else if(k==3){renderer.collectForage();toast("Has recogido hierba para el ganado +2");}
            else if(k==4 || k==5){
                int feedResult=renderer.feedNearestAnimal();
                if(feedResult==AnimalSystem.FED) toast("Has alimentado al animal. El ganado está mejor cuidado.");
                else if(feedResult==AnimalSystem.NO_FORAGE) toast("No tienes forraje. Recoge hierba de los matorrales.");
                else if(feedResult==AnimalSystem.NOT_HUNGRY) toast("Este animal está bien alimentado.");
                else toast("Acércate a una vaca o al toro.");
            }
            else if(k==6){
                if(renderer.isStableBuilt()){
                    showStable();
                } else {
                    int buildResult=renderer.buildStable(wood,stone);
                    if(buildResult==BuildingSystem.BUILT){
                        wood-=BuildingSystem.STABLE_WOOD_COST;
                        stone-=BuildingSystem.STABLE_STONE_COST;
                        toast("¡Cuadra construida! Ahora puedes almacenar forraje y alimentar al ganado.");
                    } else if(buildResult==BuildingSystem.INSUFFICIENT_MATERIALS){
                        toast("Necesitas 6 de madera y 4 de piedra para construir la cuadra.");
                    } else {
                        toast("No se ha podido construir la cuadra.");
                    }
                }
            }
            else if(k==7){showMarket();}
            else if(k==8){showVillageBoard();}
            else {toast("No hay nada con lo que interactuar");}
        } else {
            toast("Acércate a un árbol, piedra, parcela, animal o tablón de trabajos");
        }
        saveProgress();
        if(status!=null) status.setText(statusText());
    }

    void showStable(){
        final int looseForage=renderer.getForage();
        final int storedHay=renderer.getStableHay();
        final int capacity=renderer.getStableHayCapacity();
        ArrayList<String> options=new ArrayList<>();
        if(looseForage>0 && storedHay<capacity){
            options.add("Guardar forraje disponible ("+looseForage+")");
        }
        options.add("Cerrar");
        new AlertDialog.Builder(this)
            .setTitle("Cuadra de Valdemonte")
            .setMessage("Heno almacenado: "+storedHay+"/"+capacity
                +"\nForraje disponible: "+looseForage
                +"\nLa cuadra consume una unidad cada 60 segundos de juego cuando el ganado necesita alimento.")
            .setItems(options.toArray(new String[options.size()]),new DialogInterface.OnClickListener(){
                @Override public void onClick(DialogInterface dialog,int which){
                    if(looseForage>0 && storedHay<capacity && which==0){
                        int moved=renderer.storeForageInStable();
                        if(moved>0) toast("Has guardado "+moved+" unidades de forraje en la cuadra.");
                        else toast("La cuadra está llena o no queda forraje.");
                    }
                    saveProgress();
                    if(status!=null) status.setText(statusText());
                }
            }).show();
    }

    void showMarket(){
        final int harvest=renderer.getHarvest();
        final boolean canSell=harvest>0;
        ArrayList<String> options=new ArrayList<>();
        if(canSell) options.add("Vender toda la cosecha ("+harvest+" unidades)");
        options.add("Comprar 3 semillas (6 monedas)");
        new AlertDialog.Builder(this)
            .setTitle("Mercado de Valdemonte")
            .setMessage("Monedas: "+renderer.getCoins()+"\nCosecha disponible: "+harvest+"\nSemillas: "+renderer.getSeeds())
            .setItems(options.toArray(new String[options.size()]),new DialogInterface.OnClickListener(){
                @Override public void onClick(DialogInterface dialog,int which){
                    if(canSell && which==0){
                        int sold=renderer.sellHarvest();
                        if(sold>0) toast("Vendiste "+sold+" productos: +"+(sold*MarketSystem.HARVEST_PRICE)+" monedas.");
                        else toast("No tienes cosecha para vender.");
                    } else {
                        if(renderer.buySeedBundle()) toast("Has comprado 3 semillas por 6 monedas.");
                        else toast("Necesitas 6 monedas y espacio para 3 semillas.");
                    }
                    saveProgress();
                    if(status!=null) status.setText(statusText());
                }
            }).show();
    }

    void showVillageBoard(){
        final VillagerSystem.Worker[] workers=renderer.getVillagers();
        final boolean canCollect=renderer.hasPendingVillagerProduction();
        ArrayList<String> options=new ArrayList<>();
        for(VillagerSystem.Worker worker:workers){
            long seconds=worker.workMs()/1000L;
            long target=worker.intervalMs()==Long.MAX_VALUE?0L:worker.intervalMs()/1000L;
            options.add(worker.name()+" · "+worker.jobName()+" · Nivel "+worker.level()
                +" · Progreso "+seconds+"/"+target+" s");
        }
        if(canCollect) options.add("Recoger producción: "+renderer.getVillagerPendingSummary());
        options.add("Cerrar");
        final int collectIndex=canCollect?workers.length:-1;
        new AlertDialog.Builder(this)
            .setTitle("Tablón de trabajos de Valdemonte")
            .setMessage("Asigna oficios a los tres aldeanos. Trabajan mientras la partida está activa y mejoran sus habilidades con la experiencia.\n\nProducción pendiente: "+renderer.getVillagerPendingSummary())
            .setItems(options.toArray(new String[options.size()]),new DialogInterface.OnClickListener(){
                @Override public void onClick(DialogInterface dialog,int which){
                    if(which>=0 && which<workers.length){
                        showVillagerJobPicker(which);
                    } else if(canCollect && which==collectIndex){
                        VillagerSystem.Production goods=renderer.collectVillagerProduction();
                        wood+=goods.wood();
                        renderer.addForage(goods.forage());
                        renderer.addHarvest(goods.harvest());
                        if(goods.total()>0){
                            toast("Producción recogida: madera "+goods.wood()+", forraje "+goods.forage()+", cosecha "+goods.harvest()+".");
                        } else {
                            toast("No tienes espacio en los inventarios. La producción sigue en el tablón.");
                        }
                        saveProgress();
                        if(status!=null) status.setText(statusText());
                    }
                }
            }).show();
    }

    void showVillagerJobPicker(final int workerIndex){
        final String[] jobs={
            "Sin trabajo",
            "Agricultor · 2 cosechas cada 90 segundos",
            "Ganadero · 2 forrajes cada 60 segundos",
            "Leñador · 2 maderas cada 75 segundos"
        };
        new AlertDialog.Builder(this)
            .setTitle("Asignar oficio")
            .setItems(jobs,new DialogInterface.OnClickListener(){
                @Override public void onClick(DialogInterface dialog,int which){
                    if(renderer.assignVillager(workerIndex,which)){
                        toast("Trabajo actualizado. La experiencia y el nivel se conservan.");
                        saveProgress();
                        if(status!=null) status.setText(statusText());
                    }
                }
            }).show();
    }

    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    class Hud extends View {
        Paint p=new Paint(3);
        float jx=0,jy=0,baseX=0,baseY=0;
        boolean joy=false;
        int touchMode=0; // 1 joystick, 2 camera, 3 action button, 4 jump button
        Runnable action,jump;
        Hud(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setOnAction(Runnable r){action=r;}
        void setOnJump(Runnable r){jump=r;}

        protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            p.setColor(0x665c5043); c.drawCircle(105,h-115,68,p);
            p.setColor(0x99ffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(105,h-115,68,p);p.setStyle(Paint.Style.FILL);
            c.drawCircle(105+jx*38,h-115+jy*38,27,p);
            button(c,w-95,h-100,48,"HACER",0xff6d8c55);
            button(c,w-205,h-75,34,"SALTAR",0xff6b7e9b);
            p.setColor(Color.WHITE);p.setTextSize(16);c.drawText("Joystick para moverte • Arrastra para mirar",w/2-170,h-25,p);
        }

        void button(Canvas c,float x,float y,float r,String s,int color){
            p.setColor(color);c.drawCircle(x,y,r,p);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(s.length()>5?11:13);
            c.drawText(s,x,y+4,p);p.setTextAlign(Paint.Align.LEFT);
        }

        boolean insideCircle(float x,float y,float cx,float cy,float radius){
            float dx=x-cx,dy=y-cy;
            return dx*dx+dy*dy<=radius*radius;
        }

        public boolean onTouchEvent(android.view.MotionEvent e){
            float x=e.getX(),y=e.getY(),w=getWidth(),h=getHeight();
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                if(x<230&&y>h-240){
                    joy=true;touchMode=1;baseX=105;baseY=h-115;updateJoy(x,y);return true;
                }
                if(insideCircle(x,y,w-95,h-100,52)){
                    touchMode=3;if(action!=null)action.run();return true;
                }
                if(insideCircle(x,y,w-205,h-75,38)){
                    touchMode=4;if(jump!=null)jump.run();return true;
                }
                touchMode=2;renderer.startLook(x,y);return true;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE){
                if(touchMode==1){updateJoy(x,y);}
                else if(touchMode==2){renderer.look(x,y);}
                return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){
                if(touchMode==1){joy=false;jx=jy=0;renderer.setMove(0,0);}
                touchMode=0;invalidate();return true;
            }
            return true;
        }

        void updateJoy(float x,float y){
            jx=Math.max(-1,Math.min(1,(x-baseX)/58));
            jy=Math.max(-1,Math.min(1,(y-baseY)/58));
            renderer.setMove(jx,jy);invalidate();
        }
    }

    static class GameRenderer implements GLSurfaceView.Renderer {
        FloatBuffer vb;
        int program,pos,normalAttr,color,mvp,normalMatrixUniform;
        FloatBuffer sphereBuffer;
        int sphereVertexCount;
        float[] proj=new float[16],view=new float[16],model=new float[16],mvpM=new float[16];
        float[] normalTransform=new float[16],normalMatrixM=new float[16];
        volatile float moveX,moveY,px=0,pz=5,yaw=0,pitch=0;
        float vy=0,py=0,lastX,lastY;
        float playerFacing=0f,playerWalkPhase=0f;
        boolean playerMoving=false;
        boolean jumping=false;
        long lastFrameNanos=0;
        final CropSystem crops=new CropSystem();
        final AnimalSystem animals=new AnimalSystem();
        ArrayList<float[]> objects=new ArrayList<>();
        final BuildingSystem buildings=new BuildingSystem();
        final MarketSystem market=new MarketSystem();
        final VillagerSystem villagers=new VillagerSystem();
        final VillagerWorldSystem villagerWorld=new VillagerWorldSystem();
        final SeasonSystem seasons=new SeasonSystem();
        final float[] modelViewMatrix=new float[16];

        GameRenderer(){
            for(int i=0;i<9;i++) objects.add(new float[]{-7+i*1.7f,-3-(i%3)*2,1,1});
            objects.add(new float[]{5,-4,2,1});
            objects.add(new float[]{-3,-7,2,1});
            objects.add(new float[]{3,-1,4,1});
            objects.add(new float[]{6,-2,5,1});
            objects.add(new float[]{-7,-1,3,1});
            objects.add(new float[]{0,-9,3,1});
            objects.add(new float[]{8,2,3,1});
            objects.add(new float[]{-5,-10,6,1});
            objects.add(new float[]{10,-10,7,1});
            objects.add(new float[]{-10,4,8,1});
        }

        public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            GLES20.glClearColor(.56f,.72f,.83f,1);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            String vs="attribute vec3 aPos; attribute vec3 aNormal; uniform mat4 uMVP; uniform mat4 uNormalMatrix; varying float vLight; void main(){vec3 n=normalize(mat3(uNormalMatrix)*aNormal); vec3 lightDir=normalize(vec3(-0.35,0.82,0.44)); vLight=0.52+0.48*max(dot(n,lightDir),0.0); gl_Position=uMVP*vec4(aPos,1.0);} ";
            String fs="precision mediump float; uniform vec4 uColor; varying float vLight; void main(){gl_FragColor=vec4(uColor.rgb*vLight,uColor.a);}";
            int v=shader(GLES20.GL_VERTEX_SHADER,vs),f=shader(GLES20.GL_FRAGMENT_SHADER,fs);
            program=GLES20.glCreateProgram();
            GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f);GLES20.glLinkProgram(program);
            pos=GLES20.glGetAttribLocation(program,"aPos");
            normalAttr=GLES20.glGetAttribLocation(program,"aNormal");
            color=GLES20.glGetUniformLocation(program,"uColor");
            mvp=GLES20.glGetUniformLocation(program,"uMVP");
            normalMatrixUniform=GLES20.glGetUniformLocation(program,"uNormalMatrix");
            float[] cube={
                -.5f,-.5f,-.5f, .5f,-.5f,-.5f, .5f,.5f,-.5f, -.5f,-.5f,-.5f, .5f,.5f,-.5f, -.5f,.5f,-.5f,
                -.5f,-.5f,.5f, .5f,.5f,.5f, .5f,-.5f,.5f, -.5f,-.5f,.5f, -.5f,.5f,.5f, .5f,.5f,.5f,
                -.5f,-.5f,-.5f, -.5f,.5f,-.5f, -.5f,.5f,.5f, -.5f,-.5f,-.5f, -.5f,.5f,.5f, -.5f,-.5f,.5f,
                .5f,-.5f,-.5f, .5f,.5f,.5f, .5f,.5f,-.5f, .5f,-.5f,-.5f, .5f,-.5f,.5f, .5f,.5f,.5f,
                -.5f,.5f,-.5f, .5f,.5f,-.5f, .5f,.5f,.5f, -.5f,.5f,-.5f, .5f,.5f,.5f, -.5f,.5f,.5f,
                -.5f,-.5f,-.5f, .5f,-.5f,.5f, .5f,-.5f,-.5f, -.5f,-.5f,-.5f, -.5f,-.5f,.5f, .5f,-.5f,.5f
            };
            float[][] faceNormals={
                {0f,0f,-1f},{0f,0f,1f},{-1f,0f,0f},
                {1f,0f,0f},{0f,1f,0f},{0f,-1f,0f}
            };
            vb=ByteBuffer.allocateDirect(cube.length*2*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            for(int vertex=0;vertex<cube.length/3;vertex++){
                int face=vertex/6;
                vb.put(cube[vertex*3]).put(cube[vertex*3+1]).put(cube[vertex*3+2]);
                vb.put(faceNormals[face][0]).put(faceNormals[face][1]).put(faceNormals[face][2]);
            }
            vb.position(0);
            sphereBuffer=createSphereBuffer(10,16);
            sphereVertexCount=sphereBuffer.limit()/6;
        }

        FloatBuffer createSphereBuffer(int stacks,int slices){
            ArrayList<Float> data=new ArrayList<>();
            for(int stack=0;stack<stacks;stack++){
                float lat0=(float)(-Math.PI*.5+Math.PI*stack/stacks);
                float lat1=(float)(-Math.PI*.5+Math.PI*(stack+1)/stacks);
                for(int slice=0;slice<slices;slice++){
                    float lon0=(float)(Math.PI*2.0*slice/slices);
                    float lon1=(float)(Math.PI*2.0*(slice+1)/slices);
                    float[] a=spherePoint(lat0,lon0);
                    float[] b=spherePoint(lat1,lon0);
                    float[] c=spherePoint(lat1,lon1);
                    float[] d=spherePoint(lat0,lon1);
                    addSphereVertex(data,a);addSphereVertex(data,b);addSphereVertex(data,c);
                    addSphereVertex(data,a);addSphereVertex(data,c);addSphereVertex(data,d);
                }
            }
            FloatBuffer buffer=ByteBuffer.allocateDirect(data.size()*4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
            for(Float value:data)buffer.put(value);
            buffer.position(0);
            return buffer;
        }

        float[] spherePoint(float latitude,float longitude){
            float ring=(float)Math.cos(latitude);
            return new float[]{
                ring*(float)Math.cos(longitude),
                (float)Math.sin(latitude),
                ring*(float)Math.sin(longitude)
            };
        }

        void addSphereVertex(ArrayList<Float> data,float[] point){
            data.add(point[0]);data.add(point[1]);data.add(point[2]);
            data.add(point[0]);data.add(point[1]);data.add(point[2]);
        }

        int shader(int type,String src){
            int s=GLES20.glCreateShader(type);
            GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);
            return s;
        }

        public void onSurfaceChanged(GL10 gl,int w,int h){
            GLES20.glViewport(0,0,w,h);
            Matrix.perspectiveM(proj,0,62,(float)w/h,.1f,120f);
        }

        void setMove(float x,float y){moveX=x;moveY=y;}
        void startLook(float x,float y){lastX=x;lastY=y;}
        void look(float x,float y){
            yaw+=(x-lastX)*.006f;
            pitch=Math.max(-.2f,Math.min(.55f,pitch+(y-lastY)*.002f));
            lastX=x;lastY=y;
        }
        void jump(){if(!jumping){vy=.13f;jumping=true;}}

        public void onDrawFrame(GL10 gl){
            long now=System.nanoTime();
            float deltaSeconds=lastFrameNanos==0?0f:Math.max(0f,Math.min(.05f,(now-lastFrameNanos)/1000000000f));
            long delta=(long)(deltaSeconds*1000f);
            if(lastFrameNanos!=0){
                seasons.update(delta);
                crops.update(delta, seasons.getSeason(), seasons.getWeather());
                animals.update(delta, seasons.getSeason(), seasons.getWeather());
                buildings.updateStable(delta,animals);
                villagers.update(delta);
                villagerWorld.update(delta, villagers.snapshot());
            }
            lastFrameNanos=now;

            GLES20.glClearColor(seasons.skyRed(),seasons.skyGreen(),seasons.skyBlue(),1f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            float moveLength=(float)Math.hypot(moveX,moveY);
            playerMoving=moveLength>.08f;
            float moveWorldX=(float)(Math.sin(yaw)*(-moveY)+Math.cos(yaw)*moveX);
            float moveWorldZ=(float)(Math.cos(yaw)*(-moveY)-Math.sin(yaw)*moveX);
            if(playerMoving){
                float speed=4.2f*deltaSeconds;
                px+=moveWorldX*speed;
                pz+=moveWorldZ*speed;
                playerFacing=(float)Math.atan2(moveWorldX,moveWorldZ);
            }
            playerWalkPhase+=deltaSeconds*(playerMoving?9.5f:1.35f);
            if(jumping){py+=vy;vy-=.008f;if(py<=0){py=0;jumping=false;}}
            ThirdPersonCamera.Pose camera=ThirdPersonCamera.follow(px,pz,py,yaw,pitch);
            Matrix.setLookAtM(view,0,camera.x(),camera.y(),camera.z(),px,camera.targetY(),pz,0,1,0);

            draw(0,-.35f,0,45,.5f,45,.25f,.42f,.23f);
            draw(-10,-.12f,-4,2,.12f,32,.18f,.48f,.62f);
            draw(0,-.08f,6,2,.12f,28,.55f,.42f,.25f);
            for(int i=0;i<8;i++)draw(-23+i*7,-1,-24,8,5,5,.25f,.43f,.28f);

            for(float[] o:objects){
                if(o[3]==0)continue;
                int t=(int)o[2];
                if(t==1){
                    draw(o[0],.55f,o[1],.35f,1.5f,.35f,.34f,.22f,.12f);
                    draw(o[0],1.8f,o[1],1.8f,2,1.8f,seasons.foliageRed(),seasons.foliageGreen(),seasons.foliageBlue());
                }
                if(t==2)draw(o[0],.35f,o[1],1.1f,.7f,1,.48f,.48f,.46f);
                if(t==3){
                    draw(o[0],.34f,o[1],1.15f,.42f,.9f,.18f,.39f,.13f);
                    draw(o[0]-.25f,.52f,o[1]+.18f,.48f,.34f,.48f,.23f,.48f,.16f);
                }
                if(t==4){
                    draw(o[0],.55f,o[1],1.2f,.8f,1.8f,.83f,.82f,.72f);
                    draw(o[0]+.45f,.9f,o[1]-.2f,.55f,.6f,.55f,.82f,.82f,.72f);
                }
                if(t==5){
                    draw(o[0],.55f,o[1],1.35f,.9f,1.9f,.48f,.34f,.22f);
                    draw(o[0]+.5f,.9f,o[1]-.2f,.58f,.65f,.58f,.48f,.34f,.22f);
                }
                if(t==6){
                    if(buildings.isStableBuilt()){
                        draw(o[0],.68f,o[1],2.3f,1.35f,2.3f,.48f,.32f,.18f);
                        draw(o[0],1.48f,o[1],2.6f,.32f,2.6f,.30f,.16f,.11f);
                        draw(o[0],.48f,o[1]-1.16f,.72f,.92f,.10f,.22f,.13f,.08f);
                        int hayBales=Math.min(3,(buildings.getStoredHay()+9)/10);
                        for(int bale=0;bale<hayBales;bale++){
                            draw(o[0]-.48f+bale*.46f,.38f,o[1]+.48f,.38f,.38f,.38f,.76f,.62f,.22f);
                        }
                    } else {
                        draw(o[0],.12f,o[1],2.5f,.18f,2.5f,.46f,.34f,.20f);
                        draw(o[0]-1.05f,.62f,o[1]-1.05f,.16f,1.1f,.16f,.38f,.24f,.13f);
                        draw(o[0]+1.05f,.62f,o[1]-1.05f,.16f,1.1f,.16f,.38f,.24f,.13f);
                        draw(o[0]-1.05f,.62f,o[1]+1.05f,.16f,1.1f,.16f,.38f,.24f,.13f);
                        draw(o[0]+1.05f,.62f,o[1]+1.05f,.16f,1.1f,.16f,.38f,.24f,.13f);
                    }
                }
                if(t==7){
                    draw(o[0],.52f,o[1],2.2f,.75f,1.15f,.43f,.27f,.14f);
                    draw(o[0],1.12f,o[1],2.35f,.24f,1.3f,.68f,.25f,.16f);
                    draw(o[0]-.82f,.3f,o[1],.12f,.6f,.12f,.3f,.2f,.12f);
                    draw(o[0]+.82f,.3f,o[1],.12f,.6f,.12f,.3f,.2f,.12f);
                }
                if(t==8){
                    draw(o[0],.48f,o[1],1.65f,.72f,.68f,.43f,.28f,.16f);
                    draw(o[0]-.62f,.95f,o[1],.12f,.55f,.12f,.30f,.19f,.10f);
                    draw(o[0]+.62f,.95f,o[1],.12f,.55f,.12f,.30f,.19f,.10f);
                    draw(o[0],1.48f,o[1]-.34f,1.75f,.92f,.12f,.52f,.36f,.20f);
                    draw(o[0],1.5f,o[1]-.42f,1.35f,.10f,.06f,.83f,.70f,.38f);
                }
            }

            for(CropSystem.Plot plot:crops.snapshot()){
                draw(plot.x(),.1f,plot.z(),2,.18f,2,.32f,.22f,.10f);
                if(plot.stage()>CropSystem.EMPTY){
                    float height=plot.stage()==CropSystem.PLANTED?.28f:(plot.stage()==CropSystem.GROWING?.58f:.9f);
                    float green=plot.stage()==CropSystem.READY?.68f:.42f;
                    for(int j=0;j<3;j++){
                        float x=plot.x()-.55f+j*.55f;
                        draw(x,.22f+height*.5f,plot.z(),.16f,height,.16f,.16f,green,.12f);
                        if(plot.stage()==CropSystem.READY){
                            draw(x,.22f+height,plot.z(),.24f,.18f,.24f,.82f,.68f,.18f);
                        }
                    }
                }
            }

            if(animals.hasCalf()){
                draw(4.1f,.38f,-1.6f,.75f,.55f,.95f,.78f,.73f,.62f);
                draw(4.35f,.62f,-1.78f,.38f,.38f,.38f,.78f,.73f,.62f);
            }

            drawVillagers();
            draw(7,.7f,-7,3.5f,1.5f,3,.56f,.43f,.29f);
            draw(7,1.75f,-7,4,.7f,3.4f,.35f,.18f,.14f);
            drawPlayer();
        }

        // Personaje rural 3D: silueta humana, ropa por capas, cabeza esferica y mochila.
        void drawPlayer(){
            float bob=VillagerAnimationSystem.bodyBob(playerWalkPhase,playerMoving);
            float leftLeg=VillagerAnimationSystem.legAngle(playerWalkPhase,playerMoving,true);
            float rightLeg=VillagerAnimationSystem.legAngle(playerWalkPhase,playerMoving,false);
            float leftArm=VillagerAnimationSystem.armAngle(playerWalkPhase,playerMoving,true);
            float rightArm=VillagerAnimationSystem.armAngle(playerWalkPhase,playerMoving,false);

            // Sombra de contacto suave bajo los pies (sin textura externa).
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            drawMesh(sphereBuffer,sphereVertexCount,px,.012f,pz,.52f,.014f,.34f,
                .045f,.055f,.045f,.28f,playerFacing,0f);
            GLES20.glDisable(GLES20.GL_BLEND);

            // Botas, pantalones y rodillas.
            drawPlayerPart(-.13f,.14f,.09f,.23f,.16f,.35f,.12f,.085f,.055f);
            drawPlayerPart(.13f,.14f,.09f,.23f,.16f,.35f,.12f,.085f,.055f);
            drawPlayerLimb(-.13f,.61f,0f,.17f,.50f,.18f,.14f,.17f,.15f,leftLeg);
            drawPlayerLimb(.13f,.61f,0f,.17f,.50f,.18f,.14f,.17f,.15f,rightLeg);

            // Chaqueta de campo, bajo, cuello, cremallera y bolsillos.
            drawPlayerPart(0f,.99f+bob,0f,.52f,.64f,.32f,.15f,.36f,.23f);
            drawPlayerPart(0f,.72f+bob,.012f,.53f,.12f,.33f,.11f,.27f,.17f);
            drawPlayerPart(0f,1.31f+bob,.015f,.19f,.14f,.20f,.72f,.69f,.55f);
            drawPlayerPart(0f,1.00f+bob,.166f,.025f,.45f,.014f,.76f,.70f,.52f);
            drawPlayerPart(-.15f,.98f+bob,.17f,.12f,.13f,.025f,.20f,.42f,.27f);
            drawPlayerPart(.15f,.98f+bob,.17f,.12f,.13f,.025f,.20f,.42f,.27f);

            // Hombros redondeados y mangas articuladas.
            drawPlayerSphere(-.29f,1.20f+bob,0f,.16f,.16f,.16f,.15f,.36f,.23f,1f);
            drawPlayerSphere(.29f,1.20f+bob,0f,.16f,.16f,.16f,.15f,.36f,.23f,1f);
            drawPlayerLimb(-.31f,1.20f+bob,0f,.15f,.44f,.17f,.17f,.39f,.25f,leftArm);
            drawPlayerLimb(.31f,1.20f+bob,0f,.15f,.44f,.17f,.17f,.39f,.25f,rightArm);
            drawPlayerSphere(-.32f,.82f+bob,.025f,.09f,.11f,.09f,.75f,.54f,.38f,1f);
            drawPlayerSphere(.32f,.82f+bob,.025f,.09f,.11f,.09f,.75f,.54f,.38f,1f);

            // Cuello y cabeza ovalada: proporciones humanas y rasgos faciales.
            drawPlayerPart(0f,1.35f+bob,.015f,.17f,.18f,.17f,.72f,.51f,.36f);
            drawPlayerSphere(0f,1.57f+bob,.005f,.30f,.35f,.28f,.79f,.59f,.43f,1f);
            drawPlayerSphere(-.30f,1.57f+bob,.005f,.065f,.095f,.075f,.72f,.51f,.36f,1f);
            drawPlayerSphere(.30f,1.57f+bob,.005f,.065f,.095f,.075f,.72f,.51f,.36f,1f);
            drawPlayerSphere(0f,1.53f+bob,.245f,.045f,.055f,.045f,.68f,.46f,.32f,1f);
            drawPlayerSphere(-.095f,1.60f+bob,.238f,.035f,.027f,.018f,.07f,.055f,.04f,1f);
            drawPlayerSphere(.095f,1.60f+bob,.238f,.035f,.027f,.018f,.07f,.055f,.04f,1f);

            // Pelo oscuro, gorra de trabajo y mochila con correas.
            drawPlayerSphere(0f,1.72f+bob,-.045f,.31f,.17f,.29f,.12f,.075f,.045f,1f);
            drawPlayerSphere(0f,1.82f+bob,.005f,.34f,.085f,.31f,.22f,.17f,.09f,1f);
            drawPlayerPart(0f,1.02f+bob,-.205f,.34f,.42f,.15f,.30f,.19f,.11f);
            drawPlayerPart(-.19f,1.16f+bob,-.115f,.055f,.38f,.055f,.11f,.23f,.15f);
            drawPlayerPart(.19f,1.16f+bob,-.115f,.055f,.38f,.055f,.11f,.23f,.15f);
        }

        void drawPlayerSphere(float localX,float y,float localZ,float sx,float sy,float sz,
                              float r,float g,float b,float alpha){
            float c=(float)Math.cos(playerFacing);
            float s=(float)Math.sin(playerFacing);
            float x=px+localX*c+localZ*s;
            float z=pz-localX*s+localZ*c;
            drawMesh(sphereBuffer,sphereVertexCount,x,y+py,z,sx,sy,sz,r,g,b,alpha,playerFacing,0f);
        }

        void drawPlayerPart(float localX,float y,float localZ,float sx,float sy,float sz,
                            float r,float g,float b){
            float c=(float)Math.cos(playerFacing);
            float s=(float)Math.sin(playerFacing);
            float x=px+localX*c+localZ*s;
            float z=pz-localX*s+localZ*c;
            drawFacingWithLocalX(x,y+py,z,sx,sy,sz,r,g,b,playerFacing,0f);
        }

        void drawPlayerLimb(float pivotX,float pivotY,float pivotZ,float sx,float sy,float sz,
                            float r,float g,float b,float angle){
            float halfLength=sy*.5f;
            float centerY=pivotY+VillagerAnimationSystem.limbCenterYOffset(halfLength,angle);
            float centerZ=pivotZ+VillagerAnimationSystem.limbCenterZOffset(halfLength,angle);
            drawPlayerPartRotated(pivotX,centerY,centerZ,sx,sy,sz,r,g,b,angle);
        }

        void drawPlayerPartRotated(float localX,float y,float localZ,float sx,float sy,float sz,
                                   float r,float g,float b,float rotationX){
            float c=(float)Math.cos(playerFacing);
            float s=(float)Math.sin(playerFacing);
            float x=px+localX*c+localZ*s;
            float z=pz-localX*s+localZ*c;
            drawFacingWithLocalX(x,y+py,z,sx,sy,sz,r,g,b,playerFacing,rotationX);
        }

        void drawVillagers(){
            for(VillagerWorldSystem.Position villager:villagerWorld.snapshot()){
                float phase=villager.walkPhase();
                float bodyBob=VillagerAnimationSystem.bodyBob(phase,villager.walking());
                float leftLeg=VillagerAnimationSystem.legAngle(phase,villager.walking(),true);
                float rightLeg=VillagerAnimationSystem.legAngle(phase,villager.walking(),false);
                float leftArm=VillagerAnimationSystem.armAngle(phase,villager.walking(),true);
                float rightArm=VillagerAnimationSystem.armAngle(phase,villager.walking(),false);
                int job=villager.job();
                float shirtR=.43f,shirtG=.36f,shirtB=.27f;
                if(job==VillagerSystem.FARMER){shirtR=.25f;shirtG=.53f;shirtB=.24f;}
                else if(job==VillagerSystem.RANCHER){shirtR=.24f;shirtG=.40f;shirtB=.68f;}
                else if(job==VillagerSystem.WOODCUTTER){shirtR=.62f;shirtG=.34f;shirtB=.17f;}
                drawVillagerPart(villager,0f,.78f+bodyBob,0f,.36f,.55f,.27f,shirtR,shirtG,shirtB);
                drawVillagerPart(villager,0f,1.19f+bodyBob,0f,.27f,.28f,.27f,.76f,.59f,.43f);
                drawVillagerLimb(villager,-.11f,.48f,0f,.13f,.42f,.14f,.19f,.20f,.22f,leftLeg);
                drawVillagerLimb(villager,.11f,.48f,0f,.13f,.42f,.14f,.19f,.20f,.22f,rightLeg);
                drawVillagerLimb(villager,-.24f,.96f,0f,.12f,.40f,.13f,shirtR,shirtG,shirtB,leftArm);
                drawVillagerLimb(villager,.24f,.96f,0f,.12f,.40f,.13f,shirtR,shirtG,shirtB,rightArm);
                if(job==VillagerSystem.FARMER){
                    drawVillagerPart(villager,0f,1.39f+bodyBob,0f,.36f,.10f,.36f,.78f,.63f,.24f);
                } else {
                    drawVillagerPart(villager,0f,1.35f+bodyBob,0f,.29f,.09f,.29f,.24f,.18f,.12f);
                }
            }
        }

        void drawVillagerLimb(VillagerWorldSystem.Position villager,float pivotX,float pivotY,float pivotZ,
                              float sx,float sy,float sz,float r,float g,float b,float angle){
            float halfLength=sy*.5f;
            float centerY=pivotY+VillagerAnimationSystem.limbCenterYOffset(halfLength,angle);
            float centerZ=pivotZ+VillagerAnimationSystem.limbCenterZOffset(halfLength,angle);
            drawVillagerPartRotated(villager,pivotX,centerY,centerZ,sx,sy,sz,r,g,b,angle);
        }

        void drawVillagerPart(VillagerWorldSystem.Position villager,float localX,float y,float localZ,
                              float sx,float sy,float sz,float r,float g,float b){
            drawVillagerPartRotated(villager,localX,y,localZ,sx,sy,sz,r,g,b,0f);
        }

        void drawVillagerPartRotated(VillagerWorldSystem.Position villager,float localX,float y,float localZ,
                                     float sx,float sy,float sz,float r,float g,float b,float localRotationX){
            float c=(float)Math.cos(villager.facing());
            float s=(float)Math.sin(villager.facing());
            float x=villager.x()+localX*c+localZ*s;
            float z=villager.z()-localX*s+localZ*c;
            drawFacingWithLocalX(x,y,z,sx,sy,sz,r,g,b,villager.facing(),localRotationX);
        }

        void draw(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b){
            drawFacing(x,y,z,sx,sy,sz,r,g,b,0f);
        }

        void drawFacing(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b,float angle){
            drawFacingWithLocalX(x,y,z,sx,sy,sz,r,g,b,angle,0f);
        }

        void drawFacingWithLocalX(float x,float y,float z,float sx,float sy,float sz,
                                  float r,float g,float b,float angle,float localRotationX){
            drawMesh(vb,36,x,y,z,sx,sy,sz,r,g,b,1f,angle,localRotationX);
        }

        void drawMesh(FloatBuffer mesh,int vertexCount,float x,float y,float z,
                      float sx,float sy,float sz,float r,float g,float b,float alpha,
                      float angle,float localRotationX){
            if(mesh==null || vertexCount<=0)return;
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,x,y,z);
            Matrix.rotateM(model,0,(float)Math.toDegrees(angle),0f,1f,0f);
            Matrix.rotateM(model,0,(float)Math.toDegrees(localRotationX),1f,0f,0f);
            Matrix.scaleM(model,0,sx,sy,sz);
            Matrix.multiplyMM(modelViewMatrix,0,view,0,model,0);
            Matrix.multiplyMM(mvpM,0,proj,0,modelViewMatrix,0);

            Matrix.setIdentityM(normalTransform,0);
            Matrix.rotateM(normalTransform,0,(float)Math.toDegrees(angle),0f,1f,0f);
            Matrix.rotateM(normalTransform,0,(float)Math.toDegrees(localRotationX),1f,0f,0f);
            Matrix.multiplyMM(normalMatrixM,0,view,0,normalTransform,0);

            GLES20.glUseProgram(program);
            GLES20.glUniformMatrix4fv(mvp,1,false,mvpM,0);
            GLES20.glUniformMatrix4fv(normalMatrixUniform,1,false,normalMatrixM,0);
            GLES20.glUniform4f(color,r,g,b,alpha);
            GLES20.glEnableVertexAttribArray(pos);
            GLES20.glEnableVertexAttribArray(normalAttr);
            mesh.position(0);
            GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,6*4,mesh);
            mesh.position(3);
            GLES20.glVertexAttribPointer(normalAttr,3,GLES20.GL_FLOAT,false,6*4,mesh);
            mesh.position(0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertexCount);
            GLES20.glDisableVertexAttribArray(pos);
            GLES20.glDisableVertexAttribArray(normalAttr);
        }

        float nearby(){        float nearby(){
            float best=999;
            for(float[] o:objects)if(o[3]>0){
                float d=(float)Math.hypot(px-o[0],pz-o[1]);
                if(d<best)best=d;
            }
            return best;
        }

        float cropDistance(){return crops.nearestDistance(px,pz);}
        int interactCrop(){return crops.interact(px,pz);}
        String getFarmSummary(){
            return "Semillas "+crops.getSeeds()+"  Cosecha "+crops.getHarvest()+"  Listos "+crops.getReadyCount();
        }
        String saveFarm(){return crops.saveState();}
        void loadFarm(String state){crops.loadState(state);}
        String getAnimalSummary(){return animals.getSummary();}
        String saveAnimals(){return animals.saveState();}
        void loadAnimals(String state){animals.loadState(state);}
        String getBuildingSummary(){return buildings.getSummary();}
        String saveBuildings(){return buildings.saveState();}
        void loadBuildings(String state){buildings.loadState(state);}
        boolean isStableBuilt(){return buildings.isStableBuilt();}
        int getForage(){return animals.getForage();}
        int getStableHay(){return buildings.getStoredHay();}
        int getStableHayCapacity(){return BuildingSystem.MAX_STABLE_HAY;}
        int storeForageInStable(){
            int stored=buildings.storeHay(animals.getForage());
            return animals.removeForage(stored);
        }
        String saveMarket(){return market.saveState();}
        void loadMarket(String state){market.loadState(state);}
        int getCoins(){return market.getCoins();}
        int getHarvest(){return crops.getHarvest();}
        int getSeeds(){return crops.getSeeds();}
        int sellHarvest(){return market.sellHarvest(crops);}
        boolean buySeedBundle(){return market.buySeedBundle(crops);}
        String saveVillagers(){return villagers.saveState();}
        void loadVillagers(String state){villagers.loadState(state);}
        String saveVillagerWorld(){return villagerWorld.saveState();}
        void loadVillagerWorld(String state){villagerWorld.loadState(state);}
        String saveSeason(){return seasons.saveState();}
        void loadSeason(String state){seasons.loadState(state);}
        String getSeasonSummary(){return seasons.getSummary();}
        String getVillagerSummary(){return villagers.getSummary();}
        String getVillagerPendingSummary(){return villagers.getPendingSummary();}
        boolean hasPendingVillagerProduction(){return villagers.hasPendingProduction();}
        VillagerSystem.Worker[] getVillagers(){return villagers.snapshot();}
        boolean assignVillager(int index,int job){return villagers.assignJob(index,job);}
        VillagerSystem.Production collectVillagerProduction(){
            return villagers.claimProduction(99,
                AnimalSystem.MAX_FORAGE-animals.getForage(),
                CropSystem.MAX_HARVEST-crops.getHarvest());
        }
        void addForage(int amount){animals.addForage(amount);}
        int addHarvest(int amount){return crops.addHarvest(amount);}

        int buildStable(int availableWood,int availableStone){
            return buildings.buildStable(availableWood,availableStone);
        }
        int feedNearestAnimal(){return animals.feedNearest(px,pz);}
        void collectForage(){animals.addForage(2);removeNearby();}

        int nearbyKind(){
            float best=999;int kind=0;
            for(float[] o:objects)if(o[3]>0){
                float d=(float)Math.hypot(px-o[0],pz-o[1]);
                if(d<best){best=d;kind=(int)o[2];}
            }
            return kind;
        }

        void removeNearby(){
            float best=999;int ix=-1;
            for(int i=0;i<objects.size();i++){
                float[] o=objects.get(i);
                if(o[3]>0){
                    float d=(float)Math.hypot(px-o[0],pz-o[1]);
                    if(d<best){best=d;ix=i;}
                }
            }
            if(ix>=0&&best<2.4f)objects.get(ix)[3]=0;
        }
    }
}
