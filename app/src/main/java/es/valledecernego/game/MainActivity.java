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
    boolean debugTelemetry=false;

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
        debugTelemetry = getIntent().getBooleanExtra("debug_telemetry", false);
        wood = preferences.getInt("wood", 0);
        stone = preferences.getInt("stone", 0);

        FrameLayout root=new FrameLayout(this);
        surface=new GLSurfaceView(this);
        surface.setEGLContextClientVersion(2);
        surface.setEGLConfigChooser(8,8,8,8,16,0);
        surface.setPreserveEGLContextOnPause(true);
        renderer=new GameRenderer();
        renderer.setDebugTelemetry(debugTelemetry);
        renderer.loadFarm(preferences.getString("farm", ""));
        renderer.loadAnimals(preferences.getString("animals", ""));
        renderer.loadBuildings(preferences.getString("buildings", ""));
        renderer.loadHouse(preferences.getString("house", ""));
        renderer.loadMarket(preferences.getString("market", ""));
        renderer.loadQuests(preferences.getString("quests", ""));
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
        Button journal=new Button(this);
        journal.setText("DIARIO · TAREAS");
        journal.setTextSize(12);
        journal.setOnClickListener(v -> showQuestJournal());
        top.addView(journal,new LinearLayout.LayoutParams(-2,-2));
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
        String text = "VALLE DE CERNEGO 0.23.0  •  " + renderer.getSeasonSummary() + "  •  Madera " + wood + "  Piedra " + stone + "  •  Monedas " + renderer.getCoins() + "  •  " + renderer.getFarmSummary() + "  •  " + renderer.getAnimalSummary() + "  •  " + renderer.getBuildingSummary() + "  •  " + renderer.getHouseSummary() + "  •  " + renderer.getVillagerSummary() + "  •  " + renderer.getRenderStatus();
        if (debugTelemetry) text += "  •  " + renderer.getDebugTelemetry();
        return text;
    }
    void saveProgress() {
        if (preferences == null || renderer == null) return;
        preferences.edit()
            .putInt("wood", wood)
            .putInt("stone", stone)
            .putString("farm", renderer.saveFarm())
            .putString("animals", renderer.saveAnimals())
            .putString("buildings", renderer.saveBuildings())
            .putString("house", renderer.saveHouse())
            .putString("market", renderer.saveMarket())
            .putString("quests", renderer.saveQuests())
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

    void showQuestJournal(){
        new AlertDialog.Builder(this)
            .setTitle("Diario del Valle")
            .setMessage(renderer.getQuestJournal())
            .setPositiveButton("Continuar", null)
            .show();
    }

    void rewardQuest(int coins,int seeds){
        if(coins>0) renderer.addCoins(coins);
        if(seeds>0) renderer.addSeeds(seeds);
        if(coins>0 || seeds>0){
            String reward=(coins>0?coins+" monedas":"")+(coins>0 && seeds>0?" y ":"")+(seeds>0?seeds+" semillas":"");
            toast("¡Misión completada! Recompensa: "+reward);
        }
    }

    void action(){
        float cropDistance=renderer.cropDistance();
        float objectDistance=renderer.nearby();
        if(cropDistance<2.4f && cropDistance<=objectDistance){
            int result=renderer.interactCrop();
            if(result==CropSystem.PLANTED) toast("Parcela sembrada. Pulsa HACER otra vez para regarla.");
            else if(result==CropSystem.WATERED) toast("Parcela regada. El cultivo crecerá con el tiempo.");
            else if(result==CropSystem.GROWING) toast("El cultivo todavía está creciendo.");
            else if(result==CropSystem.HARVESTED){ toast("¡Cosecha recogida! +3 productos y +1 semilla."); rewardQuest(renderer.recordHarvestQuest(),0); }
            else if(result==CropSystem.OUT_OF_SEEDS) toast("No tienes semillas suficientes.");
            else toast("Acércate a una parcela de cultivo.");
        } else if(objectDistance<2.4f){
            int k=renderer.nearbyKind();
            if(k==1){wood+=2;renderer.removeNearby();toast("Has recogido madera +2");rewardQuest(renderer.recordWoodQuest(2),0);}
            else if(k==2){stone+=2;renderer.removeNearby();toast("Has recogido piedra +2");rewardQuest(renderer.recordStoneQuest(2),0);}
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
            else if(k==9){
                if(renderer.isHouseBuilt()){
                    new AlertDialog.Builder(this).setTitle("Mi casa de Cernego")
                        .setMessage("Tu casa está construida. Aquí empieza tu vida en el valle. Próximamente podrás ampliarla y decorarla.")
                        .setPositiveButton("¡A seguir!",null).show();
                } else {
                    int result=renderer.buildHouse(wood,stone);
                    if(result==HouseSystem.BUILT){
                        wood-=HouseSystem.WOOD_COST; stone-=HouseSystem.STONE_COST;
                        toast("¡Tu primera casa está construida! Ya tienes un hogar en Cernego.");
                    } else if(result==HouseSystem.INSUFFICIENT_MATERIALS){
                        toast("Para construir tu casa reúne 10 de madera y 4 de piedra.");
                    }
                }
            }
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
        // Controles dimensionados respecto a la altura real de la pantalla en landscape.
        // Evita que el HUD resulte diminuto en móviles de alta densidad o pantallas grandes.
        static final float JOY_CENTER_X_FACTOR=.20f, JOY_BOTTOM_FACTOR=.22f;
        static final float JOY_BASE_RADIUS_FACTOR=.205f, JOY_KNOB_RADIUS_FACTOR=.072f;
        static final float JOY_TOUCH_RADIUS_FACTOR=.235f, JOY_RANGE_FACTOR=.125f;
        float jx=0,jy=0,baseX=0,baseY=0;
        boolean joy=false;
        int touchMode=0; // 1 joystick, 2 camera, 3 action button, 4 jump button
        Runnable action,jump;
        long lastTouchDiagnosticNanos=0L;
        Hud(Context c){
            super(c);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            setClickable(true);
            setFocusable(true);
        }
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
            super.onSizeChanged(w,h,oldw,oldh);
            if(debugTelemetry){
                float unit=Math.min(h,w*.55f);
                android.util.Log.i("ValleCernego",String.format(Locale.US,
                    "HUD_LAYOUT width=%d height=%d joy=%.1f,%.1f touch=%.1f range=%.1f",
                    w,h,unit*JOY_CENTER_X_FACTOR,h-unit*JOY_BOTTOM_FACTOR,
                    unit*JOY_TOUCH_RADIUS_FACTOR,unit*JOY_RANGE_FACTOR));
            }
        }
        void setOnAction(Runnable r){action=r;}
        void setOnJump(Runnable r){jump=r;}

        protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            float unit=Math.min(h,w*.55f);
            float joyX=unit*JOY_CENTER_X_FACTOR, joyY=h-unit*JOY_BOTTOM_FACTOR;
            float joyBase=unit*JOY_BASE_RADIUS_FACTOR, joyKnob=unit*JOY_KNOB_RADIUS_FACTOR;
            float joyRange=unit*JOY_RANGE_FACTOR;
            p.setColor(0x775c5043); c.drawCircle(joyX,joyY,joyBase,p);
            p.setColor(0xccffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(4,unit*.006f));c.drawCircle(joyX,joyY,joyBase,p);p.setStyle(Paint.Style.FILL);
            p.setColor(0x445c5043);c.drawCircle(joyX,joyY,joyBase*.82f,p);
            p.setColor(0xffe4eadf);c.drawCircle(joyX+jx*joyRange,joyY+jy*joyRange,joyKnob,p);
            p.setColor(0xddffffff);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(3,unit*.004f));c.drawCircle(joyX+jx*joyRange,joyY+jy*joyRange,joyKnob,p);p.setStyle(Paint.Style.FILL);
            float actionX=w-unit*.145f, actionY=h-unit*.18f, actionR=unit*.112f;
            float jumpX=w-unit*.355f, jumpY=h-unit*.15f, jumpR=unit*.098f;
            button(c,actionX,actionY,actionR,"HACER",0xff6d8c55);
            button(c,jumpX,jumpY,jumpR,"SALTAR",0xff6b7e9b);
            String hint=renderer==null?"":renderer.getInteractionHint();
            if(!hint.isEmpty()){
                p.setColor(0xdd172018);p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(Math.max(15,unit*.030f));
                c.drawText(hint,actionX,actionY-actionR-14,p);
                p.setTextAlign(Paint.Align.LEFT);
            }
            p.setColor(Color.WHITE);p.setTextSize(16);c.drawText("Joystick para moverte • Arrastra para mirar",w/2-170,h-25,p);
        }

        void button(Canvas c,float x,float y,float r,String s,int color){
            p.setColor(color);c.drawCircle(x,y,r,p);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(16,r*.29f));
            c.drawText(s,x,y+4,p);p.setTextAlign(Paint.Align.LEFT);
        }

        boolean insideCircle(float x,float y,float cx,float cy,float radius){
            float dx=x-cx,dy=y-cy;
            return dx*dx+dy*dy<=radius*radius;
        }

        public boolean onTouchEvent(android.view.MotionEvent e){
            float x=e.getX(),y=e.getY(),w=getWidth(),h=getHeight();
            int event=e.getActionMasked();
            if(event==MotionEvent.ACTION_DOWN){
                float unit=Math.min(h,w*.55f);
                float joyX=unit*JOY_CENTER_X_FACTOR, joyY=h-unit*JOY_BOTTOM_FACTOR;
                if(insideCircle(x,y,joyX,joyY,unit*JOY_TOUCH_RADIUS_FACTOR)){
                    joy=true;touchMode=1;baseX=joyX;baseY=joyY;
                    updateJoy(x,y);logTouch("DOWN",x,y);return true;
                }
                if(insideCircle(x,y,w-unit*.145f,h-unit*.18f,unit*.13f)){
                    touchMode=3;if(action!=null)action.run();logTouch("ACTION",x,y);return true;
                }
                if(insideCircle(x,y,w-unit*.355f,h-unit*.15f,unit*.115f)){
                    touchMode=4;if(jump!=null)jump.run();logTouch("JUMP",x,y);return true;
                }
                touchMode=2;renderer.startLook(x,y);logTouch("LOOK_DOWN",x,y);return true;
            }
            if(event==MotionEvent.ACTION_MOVE){
                if(touchMode==1){updateJoy(x,y);logTouch("MOVE",x,y);}
                else if(touchMode==2){renderer.look(x,y);logTouch("LOOK_MOVE",x,y);}
                return true;
            }
            if(event==MotionEvent.ACTION_UP||event==MotionEvent.ACTION_CANCEL){
                int previousMode=touchMode;
                if(previousMode==1){joy=false;jx=jy=0;renderer.setMove(0,0);}
                if(previousMode!=0)logTouch("UP",x,y);
                touchMode=0;invalidate();return true;
            }
            return true;
        }

        void logTouch(String event,float x,float y){
            if(!debugTelemetry)return;
            long now=System.nanoTime();
            if("MOVE".equals(event)&&now-lastTouchDiagnosticNanos<200_000_000L)return;
            lastTouchDiagnosticNanos=now;
            android.util.Log.i("ValleCernego",TouchDiagnostic.format(event,x,y,
                getWidth(),getHeight(),touchMode,jx,jy,renderer.moveX,renderer.moveY));
        }

        void updateJoy(float x,float y){
            float range=Math.min(getHeight(),getWidth()*.55f)*JOY_RANGE_FACTOR;
            JoystickControl.Vector vector=JoystickControl.fromTouch(x,y,baseX,baseY,range);
            jx=vector.x;jy=vector.y;
            renderer.setMove(jx,jy);invalidate();
        }
    }

    static class GameRenderer implements GLSurfaceView.Renderer {
        FloatBuffer vb;
        int program,pos,normalAttr,color,mvp,normalMatrixUniform,materialStyleUniform;
        int silhouetteProgram,silhouettePos,silhouetteColor,silhouetteMvp;
        volatile boolean rendererReady=false,silhouetteReady=false,compatibilityRenderer=false;
        volatile int playerDrawCalls=0,silhouetteDrawCalls=0,playerGlError=0;
        volatile boolean playerFrameRendered=false;
        final PlayerModelSpec.Part[] silhouetteParts=PlayerModelSpec.fallbackParts();
        volatile boolean debugTelemetry=false;
        long lastTelemetryNanos=0L;
        FloatBuffer sphereBuffer,capsuleBuffer;
        int sphereVertexCount,capsuleVertexCount;
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
        final HouseSystem house=new HouseSystem();
        final MarketSystem market=new MarketSystem();
        final VillagerSystem villagers=new VillagerSystem();
        final VillagerWorldSystem villagerWorld=new VillagerWorldSystem();
        final SeasonSystem seasons=new SeasonSystem();
        final QuestSystem quests=new QuestSystem();
        int lastQuestSeedReward=0;
        final float[] modelViewMatrix=new float[16];

        GameRenderer(){
            for(float[] object:WorldLayout.initialObjects()) objects.add(object);
        }

        void setDebugTelemetry(boolean enabled){debugTelemetry=enabled;}

        public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            playerFrameRendered=false;
            playerDrawCalls=0;
            playerGlError=GLES20.GL_NO_ERROR;
            compatibilityRenderer=false;
            silhouetteReady=false;
            GLES20.glClearColor(.56f,.72f,.83f,1);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            String vs="attribute vec3 aPos; attribute vec3 aNormal; uniform mat4 uMVP; uniform mat4 uNormalMatrix; varying float vLight; varying vec3 vLocal; void main(){vec3 n=normalize(mat3(uNormalMatrix)*aNormal); vec3 lightDir=normalize(vec3(-0.35,0.82,0.44)); vLight=0.58+0.42*max(dot(n,lightDir),0.0); vLocal=aPos; gl_Position=uMVP*vec4(aPos,1.0);} ";
            String fs="precision mediump float; uniform vec4 uColor; uniform float uMaterialStyle; varying float vLight; varying vec3 vLocal; void main(){vec3 base=uColor.rgb; if(uMaterialStyle>0.5 && uMaterialStyle<1.5){base*=0.97+0.03*sin(vLocal.x*42.0)*sin(vLocal.y*42.0);} else if(uMaterialStyle>1.5 && uMaterialStyle<2.5){base*=0.98+0.02*sin((vLocal.x+vLocal.y)*56.0);} else if(uMaterialStyle>2.5 && uMaterialStyle<3.5){base*=0.95+0.05*sin(vLocal.y*18.0+vLocal.x*6.0);} else if(uMaterialStyle>3.5 && uMaterialStyle<4.5){base*=0.99+0.01*sin(vLocal.y*12.0);} else if(uMaterialStyle>4.5){base=mix(base,vec3(0.78,0.67,0.42),0.18)*(0.92+0.08*abs(sin(vLocal.x*20.0)));} gl_FragColor=vec4(base*vLight,uColor.a);}";
            program=buildProgram(vs,fs);
            if(program==0){
                android.util.Log.w("ValleCernego","Shader iluminado no disponible; se activa el shader plano de compatibilidad.");
                String fallbackVs="attribute vec3 aPos; uniform mat4 uMVP; void main(){gl_Position=uMVP*vec4(aPos,1.0);}";
                String fallbackFs="precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}";
                program=buildProgram(fallbackVs,fallbackFs);
                compatibilityRenderer=program!=0;
            }
            pos=normalAttr=color=mvp=normalMatrixUniform=materialStyleUniform=-1;
            if(program==0){
                rendererReady=false;
                android.util.Log.e("ValleCernego","Shader principal no disponible; se conservará la silueta humana independiente.");
            }else{
                pos=GLES20.glGetAttribLocation(program,"aPos");
                normalAttr=GLES20.glGetAttribLocation(program,"aNormal");
                color=GLES20.glGetUniformLocation(program,"uColor");
                mvp=GLES20.glGetUniformLocation(program,"uMVP");
                normalMatrixUniform=GLES20.glGetUniformLocation(program,"uNormalMatrix");
                materialStyleUniform=GLES20.glGetUniformLocation(program,"uMaterialStyle");
                rendererReady=pos>=0 && color>=0 && mvp>=0
                    && (normalAttr<0 || normalMatrixUniform>=0);
                if(!rendererReady) android.util.Log.e("ValleCernego","Faltan ubicaciones OpenGL obligatorias.");
            }

            // Shader independiente y mínimo para conservar una silueta humana en otros controladores.
            String silhouetteVs="attribute vec3 aPos; uniform mat4 uMVP; void main(){gl_Position=uMVP*vec4(aPos,1.0);}";
            String silhouetteFs="precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}";
            silhouetteReady=false;
            silhouetteProgram=buildProgram(silhouetteVs,silhouetteFs);
            if(silhouetteProgram!=0){
                silhouettePos=GLES20.glGetAttribLocation(silhouetteProgram,"aPos");
                silhouetteColor=GLES20.glGetUniformLocation(silhouetteProgram,"uColor");
                silhouetteMvp=GLES20.glGetUniformLocation(silhouetteProgram,"uMVP");
                silhouetteReady=silhouettePos>=0 && silhouetteColor>=0 && silhouetteMvp>=0;
            }
            if(!silhouetteReady) android.util.Log.w("ValleCernego","Silueta de compatibilidad no disponible.");
            if(!rendererReady && silhouetteReady){
                // Si falla también el shader plano inicial, reutiliza el shader mínimo
                // como renderizador principal para mantener escena y personaje visibles.
                program=silhouetteProgram;
                pos=silhouettePos;
                normalAttr=-1;
                color=silhouetteColor;
                mvp=silhouetteMvp;
                normalMatrixUniform=-1;
                materialStyleUniform=-1;
                rendererReady=true;
                compatibilityRenderer=true;
                silhouetteReady=false;
                android.util.Log.w("ValleCernego","Renderizador GLES compatible activado para la escena completa.");
            }

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
            sphereBuffer=createSphereBuffer(24,32);
            sphereVertexCount=sphereBuffer.limit()/6;
            capsuleBuffer=createCapsuleBuffer(10,24);
            capsuleVertexCount=capsuleBuffer.limit()/6;
            android.util.Log.i("ValleCernego","Render 3D listo; personaje procedural con "+PlayerModelSpec.size()+" piezas.");
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
            if(s==0)return 0;
            GLES20.glShaderSource(s,src);
            GLES20.glCompileShader(s);
            int[] status=new int[1];
            GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,status,0);
            if(status[0]==0){
                android.util.Log.e("ValleCernego","Error compilando shader: "+GLES20.glGetShaderInfoLog(s));
                GLES20.glDeleteShader(s);
                return 0;
            }
            return s;
        }

        int buildProgram(String vertexSource,String fragmentSource){
            int vertex=shader(GLES20.GL_VERTEX_SHADER,vertexSource);
            int fragment=shader(GLES20.GL_FRAGMENT_SHADER,fragmentSource);
            if(vertex==0 || fragment==0){
                if(vertex!=0)GLES20.glDeleteShader(vertex);
                if(fragment!=0)GLES20.glDeleteShader(fragment);
                return 0;
            }
            int candidate=GLES20.glCreateProgram();
            GLES20.glAttachShader(candidate,vertex);
            GLES20.glAttachShader(candidate,fragment);
            GLES20.glLinkProgram(candidate);
            int[] linked=new int[1];
            GLES20.glGetProgramiv(candidate,GLES20.GL_LINK_STATUS,linked,0);
            GLES20.glDeleteShader(vertex);
            GLES20.glDeleteShader(fragment);
            if(linked[0]==0){
                android.util.Log.e("ValleCernego","Error enlazando shader: "+GLES20.glGetProgramInfoLog(candidate));
                GLES20.glDeleteProgram(candidate);
                return 0;
            }
            return candidate;
        }

        FloatBuffer createCapsuleBuffer(int hemisphereStacks,int slices){
            ArrayList<float[]> rings=new ArrayList<>();
            for(int i=0;i<=hemisphereStacks;i++){
                float angle=(float)(-Math.PI*.5+(Math.PI*.5*i/hemisphereStacks));
                rings.add(new float[]{-.5f+.5f*(float)Math.sin(angle),.5f*(float)Math.cos(angle),(float)Math.sin(angle)});
            }
            for(int i=1;i<5;i++)rings.add(new float[]{-.5f+i*.2f,.5f,0f});
            for(int i=0;i<=hemisphereStacks;i++){
                float angle=(float)(Math.PI*.5*i/hemisphereStacks);
                rings.add(new float[]{.5f+.5f*(float)Math.sin(angle),.5f*(float)Math.cos(angle),(float)Math.sin(angle)});
            }
            ArrayList<Float> data=new ArrayList<>();
            for(int ring=0;ring<rings.size()-1;ring++){
                float[] lower=rings.get(ring),upper=rings.get(ring+1);
                for(int slice=0;slice<slices;slice++){
                    float a=(float)(Math.PI*2.0*slice/slices);
                    float b=(float)(Math.PI*2.0*(slice+1)/slices);
                    addCapsuleVertex(data,lower,a);
                    addCapsuleVertex(data,upper,a);
                    addCapsuleVertex(data,upper,b);
                    addCapsuleVertex(data,lower,a);
                    addCapsuleVertex(data,upper,b);
                    addCapsuleVertex(data,lower,b);
                }
            }
            FloatBuffer buffer=ByteBuffer.allocateDirect(data.size()*4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
            for(Float value:data)buffer.put(value);
            buffer.position(0);
            return buffer;
        }

        void addCapsuleVertex(ArrayList<Float> data,float[] ring,float longitude){
            float radial=(float)Math.sqrt(Math.max(0f,1f-ring[2]*ring[2]));
            float nx=radial*(float)Math.cos(longitude);
            float nz=radial*(float)Math.sin(longitude);
            float x=ring[1]*(float)Math.cos(longitude);
            float z=ring[1]*(float)Math.sin(longitude);
            data.add(x);data.add(ring[0]);data.add(z);
            data.add(nx);data.add(ring[2]);data.add(nz);
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

            // Restore depth/blend state before clearing and drawing the next frame.
            GLES20.glDepthMask(true);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_BLEND);
            GLES20.glClearColor(seasons.skyRed(),seasons.skyGreen(),seasons.skyBlue(),1f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
            playerMoving=JoystickControl.isMoving(moveX,moveY);
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

            draw(0,PlayerModelSpec.GROUND_LEVEL-.25f,0,100,.5f,100,.25f,.42f,.23f);
            drawRuralLandmarks();
            draw(-10,-.12f,-4,2,.12f,32,.18f,.48f,.62f);
            draw(0,-.08f,6,2,.12f,28,.55f,.42f,.25f);
            for(int i=0;i<8;i++)draw(-23+i*7,-1,-24,8,5,5,.25f,.43f,.28f);

            for(float[] o:objects){
                if(o[3]==0)continue;
                int t=(int)o[2];
                if(!WorldVisibility.isVisible(px,pz,yaw,o[0],o[1],WorldVisibility.radiusForType(t),42f))continue;
                if(t==WorldLayout.TREE){
                    drawTree(o[0],o[1]);
                }
                if(t==WorldLayout.ROCK)draw(o[0],.35f,o[1],1.1f,.7f,1,.48f,.48f,.46f);
                if(t==WorldLayout.BUSH){
                    draw(o[0],.34f,o[1],1.15f,.42f,.9f,.18f,.39f,.13f);
                    draw(o[0]-.25f,.52f,o[1]+.18f,.48f,.34f,.48f,.23f,.48f,.16f);
                }
                if(t==WorldLayout.COW) drawCow(o[0],o[1],.83f,.82f,.72f,1f,false);
                if(t==WorldLayout.CALF) drawCow(o[0],o[1],.48f,.34f,.22f,1.08f,true);
                if(t==WorldLayout.STABLE){
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
                if(t==WorldLayout.MARKET){
                    draw(o[0],.52f,o[1],2.2f,.75f,1.15f,.43f,.27f,.14f);
                    draw(o[0],1.12f,o[1],2.35f,.24f,1.3f,.68f,.25f,.16f);
                    draw(o[0]-.82f,.3f,o[1],.12f,.6f,.12f,.3f,.2f,.12f);
                    draw(o[0]+.82f,.3f,o[1],.12f,.6f,.12f,.3f,.2f,.12f);
                }
                if(t==WorldLayout.VILLAGE_BOARD){
                    draw(o[0],.48f,o[1],1.65f,.72f,.68f,.43f,.28f,.16f);
                    draw(o[0]-.62f,.95f,o[1],.12f,.55f,.12f,.30f,.19f,.10f);
                    draw(o[0]+.62f,.95f,o[1],.12f,.55f,.12f,.30f,.19f,.10f);
                    draw(o[0],1.48f,o[1]-.34f,1.75f,.92f,.12f,.52f,.36f,.20f);
                    draw(o[0],1.5f,o[1]-.42f,1.35f,.10f,.06f,.83f,.70f,.38f);
                }
                if(t==WorldLayout.HOUSE){
                    if(house.isBuilt()){
                        // Galician granite farmhouse: stone walls, slate gable roof, chimney and timber details.
                        draw(o[0],.94f,o[1],3.05f,1.82f,2.65f,.53f,.51f,.46f);
                        // Stone corner quoins and visible lintels give the facade masonry relief.
                        for(int side=-1;side<=1;side+=2){
                            draw(o[0]+side*1.43f,.98f,o[1]-1.34f,.18f,1.78f,.18f,.67f,.64f,.56f);
                            draw(o[0]+side*1.43f,.98f,o[1]+1.20f,.18f,1.78f,.18f,.67f,.64f,.56f);
                        }
                        draw(o[0],1.84f,o[1]-1.36f,3.0f,.16f,.18f,.69f,.66f,.58f);
                        // Two pitched slate roof planes meet at a raised ridge.
                        drawMesh(vb,36,o[0]-.78f,2.02f,o[1],1.78f,.18f,3.12f,.22f,.25f,.28f,1f,0f,0f,-.48f);
                        drawMesh(vb,36,o[0]+.78f,2.02f,o[1],1.78f,.18f,3.12f,.22f,.25f,.28f,1f,0f,0f,.48f);
                        draw(o[0],2.43f,o[1],.18f,.16f,3.2f,.30f,.32f,.34f);
                        // Chimney rises above the rear roof slope.
                        draw(o[0]+.82f,2.42f,o[1]+.72f,.34f,.78f,.38f,.49f,.46f,.41f);
                        draw(o[0]+.82f,2.84f,o[1]+.72f,.48f,.12f,.48f,.28f,.27f,.25f);
                        // Recessed timber door with frame, handle and threshold.
                        draw(o[0],.66f,o[1]-1.355f,.72f,1.28f,.10f,.28f,.16f,.09f);
                        draw(o[0],.67f,o[1]-1.42f,.52f,1.12f,.045f,.39f,.22f,.11f);
                        draw(o[0]+.16f,.68f,o[1]-1.45f,.045f,.08f,.025f,.82f,.65f,.28f);
                        draw(o[0],.12f,o[1]-1.43f,.94f,.12f,.22f,.63f,.61f,.54f);
                        // Blue-grey divided windows with wooden surrounds and central mullions.
                        for(int side=-1;side<=1;side+=2){
                            float wx=o[0]+side*.94f;
                            draw(wx,1.22f,o[1]-1.37f,.64f,.62f,.10f,.30f,.48f,.53f);
                            draw(wx,1.22f,o[1]-1.43f,.48f,.46f,.035f,.48f,.69f,.72f);
                            draw(wx,1.22f,o[1]-1.46f,.045f,.48f,.025f,.27f,.20f,.14f);
                            draw(wx,1.22f,o[1]-1.46f,.48f,.045f,.025f,.27f,.20f,.14f);
                        }
                        // Timber gable trim and small front flower boxes.
                        draw(o[0],1.98f,o[1]-1.38f,1.85f,.09f,.09f,.34f,.22f,.13f);
                        for(int side=-1;side<=1;side+=2){
                            draw(o[0]+side*.94f,.83f,o[1]-1.53f,.58f,.12f,.12f,.35f,.20f,.12f);
                            draw(o[0]+side*.94f,.94f,o[1]-1.53f,.48f,.12f,.10f,.24f,.43f,.18f);
                        }
                    } else {
                        draw(o[0],.10f,o[1],3.4f,.16f,3.0f,.58f,.43f,.27f);
                        draw(o[0]-1.45f,.34f,o[1],.14f,.58f,.14f,.38f,.25f,.14f);
                        draw(o[0]+1.45f,.34f,o[1],.14f,.58f,.14f,.38f,.25f,.14f);
                        draw(o[0],.30f,o[1]-1.38f,3.0f,.5f,.14f,.38f,.25f,.14f);
                    }
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
            if(debugTelemetry && now-lastTelemetryNanos>=1_000_000_000L){
                ThirdPersonCamera.Pose debugCamera=ThirdPersonCamera.follow(px,pz,py,yaw,pitch);
                android.util.Log.i("ValleCernego",String.format(Locale.US,
                    "PLAYER_STATE POS=%.2f,%.2f CAM=%.2f,%.2f PARTS=%d FALLBACK=%d GL=0x%04x",
                    px,pz,debugCamera.x(),debugCamera.z(),playerDrawCalls,silhouetteDrawCalls,playerGlError));
                lastTelemetryNanos=now;
            }
        }

        void drawRuralLandmarks(){
            // Camino de tierra que conecta la zona de inicio con la aldea.
            draw(0f,PlayerModelSpec.GROUND_LEVEL-.06f,26f,2.7f,.12f,36f,.48f,.40f,.31f);

            // Lomas verdes suaves al fondo: dan referencias de profundidad para la cámara.
            for(int i=0;i<7;i++){
                float x=-25f+i*8.4f;
                float z=43f+(i%2)*1.4f;
                if(!WorldVisibility.isVisible(px,pz,yaw,x,z,10f,58f))continue;
                drawMesh(sphereBuffer,sphereVertexCount,x,1.62f,z,9f,3.4f,8f,
                    .25f,.38f,.24f,1f,0f,0f);
            }

            // Cierre tradicional de madera a la izquierda del camino.
            for(int i=0;i<7;i++){
                float z=13f+i*4f;
                if(!WorldVisibility.isVisible(px,pz,yaw,-7.2f,z,2f,38f))continue;
                draw(-7.2f,.50f,z,.14f,.90f,.14f,.38f,.25f,.14f);
                if(i<6){
                    draw(-7.2f,.72f,z+2f,.12f,.12f,4f,.43f,.29f,.16f);
                    draw(-7.2f,.36f,z+2f,.12f,.12f,4f,.43f,.29f,.16f);
                }
            }
        }

        String getDebugTelemetry(){
            ThirdPersonCamera.Pose camera=ThirdPersonCamera.follow(px,pz,py,yaw,pitch);
            return String.format(Locale.US,"POS=%.2f,%.2f CAM=%.2f,%.2f",
                px,pz,camera.x(),camera.z());
        }

        String getRenderStatus(){
            if(!rendererReady && !silhouetteReady)return "3D NO DISPONIBLE";
            if(!playerFrameRendered)return "3D OK · esperando primer fotograma";
            if(playerGlError!=GLES20.GL_NO_ERROR){
                return "3D ERROR GL 0x"+Integer.toHexString(playerGlError)
                    +" · "+playerDrawCalls+"/"+PlayerModelSpec.size();
            }
            if(compatibilityRenderer && playerDrawCalls>=PlayerModelSpec.size()){
                return "3D COMPATIBLE · "+playerDrawCalls+" piezas · shader GLES plano · GL OK";
            }
            if(rendererReady && playerDrawCalls>=PlayerModelSpec.size()){
                return "3D OK · "+playerDrawCalls+" piezas · respaldo humanoide "+silhouetteDrawCalls+" · GL OK";
            }
            if(silhouetteReady && silhouetteDrawCalls>=silhouetteParts.length){
                return "3D COMPATIBLE · "+silhouetteDrawCalls+" piezas humanoides · GL OK";
            }
            return "3D INCOMPLETO · "+playerDrawCalls+"/"+PlayerModelSpec.size()+" piezas";
        }

        // Personaje compuesto por primitivas 3D; brazos y piernas usan cápsulas redondeadas.
        void drawPlayer(){
            if(!rendererReady && !silhouetteReady)return;
            int submittedParts=0;
            playerDrawCalls=0;
            silhouetteDrawCalls=0;
            playerGlError=GLES20.GL_NO_ERROR;
            for(int i=0;i<8;i++){
                if(GLES20.glGetError()==GLES20.GL_NO_ERROR)break;
            }

            if(rendererReady){
                // La sombra translúcida no escribe profundidad: no puede tapar las botas.
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
                GLES20.glDepthMask(false);
                drawMesh(sphereBuffer,sphereVertexCount,px,PlayerModelSpec.GROUND_LEVEL+.018f,pz,.52f,.035f,.34f,
                    .045f,.055f,.045f,.28f,playerFacing,0f);
                GLES20.glDepthMask(true);
                GLES20.glDisable(GLES20.GL_BLEND);

                float bob=VillagerAnimationSystem.bodyBob(playerWalkPhase,playerMoving);
                for(int i=0;i<PlayerModelSpec.size();i++){
                    PlayerModelSpec.Part part=PlayerModelSpec.partAt(i);
                    float angle=playerPartAngle(part);
                    PlayerModelTransform.Pose pose=PlayerModelTransform.partPose(
                        px,py,pz,playerFacing,bob,part,angle);
                    boolean submitted;
                    if(part.shape==PlayerModelSpec.Shape.LIMB){
                        submitted=drawMesh(capsuleBuffer,capsuleVertexCount,pose.x,pose.y,pose.z,
                            pose.sx,pose.sy*.5f,pose.sz,part.r,part.g,part.b,part.alpha,
                            pose.yaw,pose.pitch,pose.roll,part.materialStyle);
                    }else if(part.shape==PlayerModelSpec.Shape.SPHERE){
                        submitted=drawMesh(sphereBuffer,sphereVertexCount,pose.x,pose.y,pose.z,
                            pose.sx,pose.sy,pose.sz,part.r,part.g,part.b,part.alpha,
                            pose.yaw,pose.pitch,pose.roll,part.materialStyle);
                    }else{
                        submitted=drawMesh(vb,36,pose.x,pose.y,pose.z,pose.sx,pose.sy,pose.sz,
                            part.r,part.g,part.b,part.alpha,pose.yaw,pose.pitch,pose.roll,part.materialStyle);
                    }
                    if(submitted)submittedParts++;
                }
            }

            // Independent low-feature silhouette protects the essential human shape on mobile GPUs.
            boolean detailedComplete=rendererReady && submittedParts>=PlayerModelSpec.size();
            if(silhouetteReady)drawPlayerSilhouette(detailedComplete ? 0.18f : 1f);
            playerDrawCalls=detailedComplete?submittedParts:silhouetteDrawCalls;
            playerGlError=GLES20.glGetError();
            if(playerGlError!=GLES20.GL_NO_ERROR){
                android.util.Log.w("ValleCernego","OpenGL 0x"+Integer.toHexString(playerGlError)
                    +" al terminar el lote del personaje.");
                for(int i=0;i<8;i++){
                    if(GLES20.glGetError()==GLES20.GL_NO_ERROR)break;
                }
            }
            playerFrameRendered=playerDrawCalls>0;
        }

        float playerPartAngle(PlayerModelSpec.Part part){
            if(part.motion==PlayerModelSpec.Motion.LEFT_LEG){
                return VillagerAnimationSystem.legAngle(playerWalkPhase,playerMoving,true);
            }
            if(part.motion==PlayerModelSpec.Motion.RIGHT_LEG){
                return VillagerAnimationSystem.legAngle(playerWalkPhase,playerMoving,false);
            }
            if(part.motion==PlayerModelSpec.Motion.LEFT_ARM){
                return VillagerAnimationSystem.armAngle(playerWalkPhase,playerMoving,true);
            }
            if(part.motion==PlayerModelSpec.Motion.RIGHT_ARM){
                return VillagerAnimationSystem.armAngle(playerWalkPhase,playerMoving,false);
            }
            return 0f;
        }

        void drawPlayerSilhouette(float alpha){
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDepthMask(false);
            if(!rendererReady){
                drawSilhouetteMesh(sphereBuffer,sphereVertexCount,px,PlayerModelSpec.GROUND_LEVEL+.018f,pz,
                    .52f,.035f,.34f,.045f,.055f,.045f,.28f,playerFacing,0f,0f);
            }
            float bob=VillagerAnimationSystem.bodyBob(playerWalkPhase,playerMoving);
            for(PlayerModelSpec.Part part:silhouetteParts){
                PlayerModelTransform.Pose pose=PlayerModelTransform.partPose(
                    px,py,pz,playerFacing,bob,part,playerPartAngle(part));
                FloatBuffer mesh;
                int count;
                float sy=pose.sy;
                if(part.shape==PlayerModelSpec.Shape.LIMB){
                    mesh=capsuleBuffer;count=capsuleVertexCount;sy*=.5f;
                }else if(part.shape==PlayerModelSpec.Shape.SPHERE){
                    mesh=sphereBuffer;count=sphereVertexCount;
                }else{
                    mesh=vb;count=36;
                }
                if(drawSilhouetteMesh(mesh,count,pose.x,pose.y,pose.z,pose.sx,sy,pose.sz,
                    part.r,part.g,part.b,alpha,pose.yaw,pose.pitch,pose.roll)){
                    silhouetteDrawCalls++;
                }
            }
            GLES20.glDepthMask(true);
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        boolean drawSilhouetteMesh(FloatBuffer mesh,int vertexCount,float x,float y,float z,
                                   float sx,float sy,float sz,float r,float g,float b,float alpha,
                                   float angle,float localRotationX,float localRotationZ){
            if(!silhouetteReady || mesh==null || vertexCount<=0)return false;
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,x,y,z);
            Matrix.rotateM(model,0,(float)Math.toDegrees(angle),0f,1f,0f);
            Matrix.rotateM(model,0,(float)Math.toDegrees(localRotationX),1f,0f,0f);
            Matrix.rotateM(model,0,(float)Math.toDegrees(localRotationZ),0f,0f,1f);
            Matrix.scaleM(model,0,sx,sy,sz);
            Matrix.multiplyMM(modelViewMatrix,0,view,0,model,0);
            Matrix.multiplyMM(mvpM,0,proj,0,modelViewMatrix,0);
            GLES20.glUseProgram(silhouetteProgram);
            GLES20.glUniformMatrix4fv(silhouetteMvp,1,false,mvpM,0);
            GLES20.glUniform4f(silhouetteColor,r,g,b,alpha);
            GLES20.glEnableVertexAttribArray(silhouettePos);
            mesh.position(0);
            GLES20.glVertexAttribPointer(silhouettePos,3,GLES20.GL_FLOAT,false,6*4,mesh);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertexCount);
            GLES20.glDisableVertexAttribArray(silhouettePos);
            return true;
        }

        // Árbol de frondosa atlántica: tronco, ramas, copa en varios volúmenes y matices estacionales.
        // Roble/castaño atlántico: raíces, tronco irregular, ramas y copas superpuestas.
        // La variación es determinista para que cada árbol conserve su aspecto entre fotogramas.
        // Árbol atlántico con LOD: la copa distante usa una sola esfera para reducir draw calls.
        void drawTree(float x,float z){
            float leafR=seasons.foliageRed(),leafG=seasons.foliageGreen(),leafB=seasons.foliageBlue();
            float seed=(float)(Math.abs(Math.sin(x*12.73f+z*4.91f)));
            float crown=1f+(seed-.5f)*.18f;
            float distance=(float)Math.hypot(x-px,z-pz);
            drawMesh(capsuleBuffer,capsuleVertexCount,x,.98f,z,.20f,.98f,.20f,.27f,.16f,.085f,1f,0f,0f);
            if(distance>20f){
                drawMesh(sphereBuffer,sphereVertexCount,x,2.18f,z,.82f*crown,.88f*crown,.78f*crown,
                    leafR,leafG,leafB,1f,0f,0f);
                return;
            }
            for(int i=0;i<2;i++){
                float a=(float)(i*Math.PI+seed*.7f);
                drawMesh(capsuleBuffer,capsuleVertexCount,x+(float)Math.cos(a)*.22f,.13f,z+(float)Math.sin(a)*.22f,
                    .075f,.42f,.075f,.29f,.18f,.095f,1f,0f,0f,(float)Math.cos(a)*.72f);
            }
            drawMesh(capsuleBuffer,capsuleVertexCount,x-.30f,1.38f,z,.105f,.78f,.105f,.30f,.18f,.095f,1f,0f,0f,-.68f);
            drawMesh(capsuleBuffer,capsuleVertexCount,x+.30f,1.43f,z+.03f,.105f,.82f,.105f,.30f,.18f,.095f,1f,0f,0f,.66f);
            drawMesh(sphereBuffer,sphereVertexCount,x,2.18f,z,.76f*crown,.88f*crown,.73f*crown,
                leafR*.91f,leafG*.91f,leafB*.91f,1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x-.48f,2.02f,z+.12f,.54f,.62f,.55f,
                Math.min(1f,leafR*1.08f),Math.min(1f,leafG*1.08f),Math.min(1f,leafB*1.08f),1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x+.48f,2.06f,z-.12f,.57f,.68f,.58f,
                Math.max(0f,leafR*.86f),Math.max(0f,leafG*.90f),Math.max(0f,leafB*.86f),1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x-.16f,2.55f,z-.24f,.48f,.55f,.48f,
                Math.min(1f,leafR*1.04f),Math.min(1f,leafG*1.05f),Math.min(1f,leafB*1.04f),1f,0f,0f);
        }

        // Ganado de raza cárnica: silueta más natural, cuello, articulaciones, pezuñas y rasgos faciales.
        void drawCow(float x,float z,float r,float g,float b,float scale,boolean calf){
            float bodyY=.69f*scale, headX=.64f*scale, headY=1.04f*scale;
            float legWidth=(calf?.078f:.095f)*scale;
            drawMesh(sphereBuffer,sphereVertexCount,x,bodyY,z,.76f*scale,.46f*scale,.47f*scale,r,g,b,1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x-.08f*scale,.77f*scale,z,.61f*scale,.36f*scale,.45f*scale,
                Math.min(1f,r*1.04f),Math.min(1f,g*1.04f),Math.min(1f,b*1.04f),1f,0f,0f);
            // Pecho y cuello unidos al cuerpo para evitar una silueta de bloques.
            drawMesh(sphereBuffer,sphereVertexCount,x+.43f*scale,.82f*scale,z,.34f*scale,.43f*scale,.34f*scale,r*.96f,g*.96f,b*.96f,1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x+headX*.88f,1.00f*scale,z,.24f*scale,.34f*scale,.24f*scale,r,g,b,1f,0f,0f);
            // Manchas irregulares distribuidas a ambos lados del lomo.
            drawMesh(sphereBuffer,sphereVertexCount,x-.24f*scale,bodyY+.20f*scale,z-.39f*scale,.23f*scale,.17f*scale,.055f*scale,.17f,.15f,.13f,1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x+.20f*scale,bodyY+.16f*scale,z+.39f*scale,.25f*scale,.15f*scale,.055f*scale,.20f,.17f,.15f,1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x-.55f*scale,.73f*scale,z+.12f*scale,.15f*scale,.24f*scale,.19f*scale,.20f,.18f,.16f,1f,0f,0f);
            // Cabeza, frente, hocico rosado y mandíbula.
            drawMesh(sphereBuffer,sphereVertexCount,x+headX,headY,z,.30f*scale,.31f*scale,.29f*scale,r,g,b,1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x+(headX+.12f)*scale,.87f*scale,z-.015f*scale,.235f*scale,.145f*scale,.235f*scale,.72f,.43f,.39f,1f,0f,0f);
            drawMesh(sphereBuffer,sphereVertexCount,x+(headX+.07f)*scale,.78f*scale,z-.015f*scale,.15f*scale,.09f*scale,.16f*scale,.63f,.36f,.33f,1f,0f,0f);
            for(int side=-1;side<=1;side+=2){
                // Ojo con blanco, iris y pupila.
                drawMesh(sphereBuffer,sphereVertexCount,x+(headX+.06f)*scale,headY+.09f*scale,z+side*.255f*scale,.075f*scale,.065f*scale,.035f*scale,.94f,.91f,.82f,1f,0f,0f);
                drawMesh(sphereBuffer,sphereVertexCount,x+(headX+.085f)*scale,headY+.09f*scale,z+side*.284f*scale,.031f*scale,.042f*scale,.018f*scale,.12f,.085f,.055f,1f,0f,0f);
                drawMesh(sphereBuffer,sphereVertexCount,x+(headX-.08f)*scale,headY+.13f*scale,z+side*.32f*scale,.19f*scale,.075f*scale,.12f*scale,.66f,.47f,.40f,1f,0f,0f);
                // Orificios nasales.
                drawMesh(sphereBuffer,sphereVertexCount,x+(headX+.27f)*scale,.89f*scale,z+side*.105f*scale,.025f*scale,.022f*scale,.018f*scale,.28f,.13f,.12f,1f,0f,0f);
                if(!calf){
                    drawMesh(capsuleBuffer,capsuleVertexCount,x+(headX-.04f)*scale,headY+.34f*scale,z+side*.13f*scale,.055f*scale,.20f*scale,.055f*scale,.88f,.82f,.62f,1f,0f,0f,side*.16f);
                }
            }
            // Cuatro patas con corvejones y pezuñas oscuras.
            for(int side=-1;side<=1;side+=2){
                for(int front=-1;front<=1;front+=2){
                    float legX=x+(front*.46f)*scale,legZ=z+side*.27f*scale;
                    drawMesh(capsuleBuffer,capsuleVertexCount,legX,.32f*scale,legZ,legWidth,.36f*scale,legWidth,r*.94f,g*.94f,b*.94f,1f,0f,0f);
                    drawMesh(sphereBuffer,sphereVertexCount,legX,.105f*scale,legZ,.10f*scale,.10f*scale,.105f*scale,r*.88f,g*.88f,b*.88f,1f,0f,0f);
                    drawMesh(sphereBuffer,sphereVertexCount,legX,.045f*scale,legZ+.018f*scale,.115f*scale,.065f*scale,.13f*scale,.16f,.145f,.13f,1f,0f,0f);
                }
            }
            // Cola con mechón.
            drawMesh(capsuleBuffer,capsuleVertexCount,x-.72f*scale,.62f*scale,z,.045f*scale,.31f*scale,.045f*scale,.32f,.22f,.14f,1f,0f,0f,-.58f);
            drawMesh(sphereBuffer,sphereVertexCount,x-.84f*scale,.43f*scale,z,.075f*scale,.11f*scale,.075f,.18f,.13f,.09f,1f,0f,0f);
        }

        void drawVillagers(){
            for(VillagerWorldSystem.Position villager:villagerWorld.snapshot()){
                if(!WorldVisibility.isVisible(px,pz,yaw,villager.x(),villager.z(),1.6f,38f))continue;
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
                // Silueta humana más orgánica: torso, cuello, manos, rasgos faciales y ropa de oficio.
                drawVillagerPart(villager,0f,.78f+bodyBob,0f,.36f,.55f,.27f,shirtR,shirtG,shirtB);
                drawVillagerPart(villager,0f,1.19f+bodyBob,0f,.27f,.28f,.27f,.76f,.59f,.43f);
                drawVillagerPart(villager,0f,1.02f+bodyBob,.145f,.19f,.34f,.035f,shirtR*.78f,shirtG*.78f,shirtB*.78f);
                drawVillagerPart(villager,0f,1.04f+bodyBob,.17f,.025f,.37f,.018f,.78f,.68f,.48f);
                drawVillagerLimb(villager,-.11f,.48f,0f,.13f,.42f,.14f,.19f,.20f,.22f,leftLeg);
                drawVillagerLimb(villager,.11f,.48f,0f,.13f,.42f,.14f,.19f,.20f,.22f,rightLeg);
                drawVillagerLimb(villager,-.24f,.96f,0f,.12f,.40f,.13f,shirtR,shirtG,shirtB,leftArm);
                drawVillagerLimb(villager,.24f,.96f,0f,.12f,.40f,.13f,shirtR,shirtG,shirtB,rightArm);
                drawVillagerPart(villager,-.24f,.73f+bodyBob,.02f,.09f,.10f,.09f,.72f,.52f,.37f);
                drawVillagerPart(villager,.24f,.73f+bodyBob,.02f,.09f,.10f,.09f,.72f,.52f,.37f);
                drawVillagerPart(villager,0f,1.39f+bodyBob,0f,.30f,.32f,.28f,.76f,.59f,.43f);
                drawVillagerPart(villager,0f,1.53f+bodyBob,-.035f,.31f,.13f,.29f,.18f,.095f,.045f);
                drawVillagerPart(villager,0f,1.60f+bodyBob,.005f,.34f,.075f,.31f,.24f,.17f,.10f);
                drawVillagerPart(villager,-.075f,1.40f+bodyBob,.245f,.035f,.035f,.018f,.12f,.085f,.06f);
                drawVillagerPart(villager,.075f,1.40f+bodyBob,.245f,.035f,.035f,.018f,.12f,.085f,.06f);
                if(job==VillagerSystem.FARMER){
                    drawVillagerPart(villager,0f,1.58f+bodyBob,0f,.37f,.10f,.37f,.78f,.63f,.24f);
                    drawVillagerPart(villager,0f,1.52f+bodyBob,.16f,.29f,.07f,.035f,.56f,.42f,.18f);
                } else {
                    drawVillagerPart(villager,0f,1.57f+bodyBob,0f,.34f,.10f,.34f,.24f,.18f,.12f);
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

        boolean drawMesh(FloatBuffer mesh,int vertexCount,float x,float y,float z,
                         float sx,float sy,float sz,float r,float g,float b,float alpha,
                         float angle,float localRotationX){
            return drawMesh(mesh,vertexCount,x,y,z,sx,sy,sz,r,g,b,alpha,angle,localRotationX,0f);
        }

        boolean drawMesh(FloatBuffer mesh,int vertexCount,float x,float y,float z,
                         float sx,float sy,float sz,float r,float g,float b,float alpha,
                         float angle,float localRotationX,float localRotationZ){
            return drawMesh(mesh,vertexCount,x,y,z,sx,sy,sz,r,g,b,alpha,
                angle,localRotationX,localRotationZ,PlayerModelSpec.MATERIAL_SOLID);
        }

        boolean drawMesh(FloatBuffer mesh,int vertexCount,float x,float y,float z,
                         float sx,float sy,float sz,float r,float g,float b,float alpha,
                         float angle,float localRotationX,float localRotationZ,float materialStyle){
            if(!rendererReady || mesh==null || vertexCount<=0)return false;
            Matrix.setIdentityM(model,0);
            Matrix.translateM(model,0,x,y,z);
            Matrix.rotateM(model,0,(float)Math.toDegrees(angle),0f,1f,0f);
            Matrix.rotateM(model,0,(float)Math.toDegrees(localRotationX),1f,0f,0f);
            Matrix.rotateM(model,0,(float)Math.toDegrees(localRotationZ),0f,0f,1f);
            Matrix.scaleM(model,0,sx,sy,sz);
            Matrix.multiplyMM(modelViewMatrix,0,view,0,model,0);
            Matrix.multiplyMM(mvpM,0,proj,0,modelViewMatrix,0);

            // Inversa transpuesta analítica de la parte lineal: R * escala inversa.
            // Mantiene la iluminación coherente en las piezas rotadas y escaladas.
            Matrix.setIdentityM(normalTransform,0);
            Matrix.rotateM(normalTransform,0,(float)Math.toDegrees(angle),0f,1f,0f);
            Matrix.rotateM(normalTransform,0,(float)Math.toDegrees(localRotationX),1f,0f,0f);
            Matrix.rotateM(normalTransform,0,(float)Math.toDegrees(localRotationZ),0f,0f,1f);
            Matrix.scaleM(normalTransform,0,1f/sx,1f/sy,1f/sz);
            Matrix.multiplyMM(normalMatrixM,0,view,0,normalTransform,0);

            GLES20.glUseProgram(program);
            GLES20.glUniformMatrix4fv(mvp,1,false,mvpM,0);
            if(normalMatrixUniform>=0){
                GLES20.glUniformMatrix4fv(normalMatrixUniform,1,false,normalMatrixM,0);
            }
            GLES20.glUniform4f(color,r,g,b,alpha);
            if(materialStyleUniform>=0)GLES20.glUniform1f(materialStyleUniform,materialStyle);
            GLES20.glEnableVertexAttribArray(pos);
            mesh.position(0);
            GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,6*4,mesh);
            if(normalAttr>=0){
                GLES20.glEnableVertexAttribArray(normalAttr);
                mesh.position(3);
                GLES20.glVertexAttribPointer(normalAttr,3,GLES20.GL_FLOAT,false,6*4,mesh);
            }
            mesh.position(0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,vertexCount);
            GLES20.glDisableVertexAttribArray(pos);
            if(normalAttr>=0)GLES20.glDisableVertexAttribArray(normalAttr);
            return true;
        }

        float nearby(){
            int index=ResourceInteraction.nearestIndex(objects,px,pz,Float.POSITIVE_INFINITY);
            if(index<0)return 999f;
            float[] item=objects.get(index);
            return (float)Math.hypot(px-item[0],pz-item[1]);
        }

        String getInteractionHint(){
            int index=ResourceInteraction.nearestIndex(objects,px,pz,2.4f);
            if(index<0){
                float distance=nearby();
                return distance<999f?"ACÉRCATE A UN RECURSO":"EXPLORA PARA ENCONTRAR RECURSOS";
            }
            int kind=(int)objects.get(index)[2];
            if(kind==1)return "ÁRBOL · HACER: +2 MADERA";
            if(kind==2)return "ROCA · HACER: +2 PIEDRA";
            if(kind==3)return "MATORRAL · HACER: +2 FORRAJE";
            if(kind==4)return "VACA · HACER: ALIMENTAR";
            if(kind==5)return "TORO · HACER: ALIMENTAR";
            if(kind==6)return "CUADRA · HACER: CONSTRUIR/ABRIR";
            if(kind==7)return "MERCADO · HACER: COMERCIAR";
            if(kind==8)return "TABLÓN · HACER: OFICIOS";
            if(kind==9)return "CASA · HACER: CONSTRUIR/VER";
            return "HACER · INTERACTUAR";
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
        String getHouseSummary(){return house.isBuilt()?"Casa propia construida":"Casa pendiente";}
        boolean isHouseBuilt(){return house.isBuilt();}
        int buildHouse(int availableWood,int availableStone){return house.build(availableWood,availableStone);}
        String saveHouse(){return house.saveState();}
        void loadHouse(String state){house.loadState(state);}
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
        String saveQuests(){return quests.saveState();}
        void loadQuests(String state){quests.loadState(state);}
        String getQuestJournal(){return quests.getJournal();}
        int recordWoodQuest(int amount){return quests.recordWood(amount);}
        int recordStoneQuest(int amount){return quests.recordStone(amount);}
        int recordHarvestQuest(){return quests.recordHarvest();}
        int recordPlantQuest(){lastQuestSeedReward=quests.recordPlant();return 0;}
        int getLastQuestSeedReward(){int reward=lastQuestSeedReward;lastQuestSeedReward=0;return reward;}
        void addCoins(int amount){market.addCoins(amount);}
        void addSeeds(int amount){crops.addSeeds(amount);}
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
            int index=ResourceInteraction.nearestIndex(objects,px,pz,2.4f);
            return index<0?0:(int)objects.get(index)[2];
        }

        void removeNearby(){
            int index=ResourceInteraction.nearestIndex(objects,px,pz,2.4f);
            if(index>=0)objects.get(index)[3]=0;
        }
    }
}
