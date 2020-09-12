package com.example.pinball_wallpaper;

import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.preference.PreferenceManager;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class WallService extends WallpaperService {

    public float scale = .5f;
    // Different arrays of different types of maps
    private ArrayList<Island> islands = new ArrayList<Island>();
    private ArrayList<Cloud> clouds = new ArrayList<>();

    private int WallWidth, WallHeight, CellWidth, CellHeight;
    private int SEA_COLOR = Color.parseColor("#4e4cca"), ALPHA = 100, PAN_SPEED = 500, SHADE_AMOUNT = 0;
    private boolean TRANSPARENT_CLOUDS = false;
    private float SCALE = 1f;
    private int[] resources = new int[5];
    private int[] cloud_resources = new int[4];

    @Override
    public void onCreate() {
        super.onCreate();

        //Get Wallpaper Width and Height
        WallpaperManager manager = WallpaperManager.getInstance(getApplicationContext());
        WallWidth = manager.getDesiredMinimumWidth();
        WallHeight = manager.getDesiredMinimumHeight();
    }

    @Override
    public Engine onCreateEngine() {
        return new WallEngine();
    }

    class WallEngine extends Engine {
        private int framerate = 60;
        private final Handler handler = new Handler();
        private int PAN_SPEED, SHADE_AMOUNT;

        private final Runnable PinballRunnable = new Runnable() {
            @Override
            public void run() {
                drawFrame();
            }
        };

        public WallEngine() {
            //Load preferences and elements here
            SharedPreferences prefs  = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            PAN_SPEED = 250 * Integer.parseInt(prefs.getString("PAN_SPEED", "0"));

            int shadeAmount = Integer.parseInt(prefs.getString("SHADE_AMOUNT","0"));
            TRANSPARENT_CLOUDS = prefs.getBoolean("TRANSPARENT_CLOUDS", false);

            switch(shadeAmount) {
                case 0:
                    Log.d("SHADE AMOUNT >>>",String.valueOf(0));
                    SHADE_AMOUNT = 0;
                case 1:
                    Log.d("SHADE AMOUNT >>>",String.valueOf(1));
                    SHADE_AMOUNT = 100;
                case 2:
                    Log.d("SHADE AMOUNT >>>",String.valueOf(2));
                    SHADE_AMOUNT = 170;
                case 3:
                    Log.d("SHADE AMOUNT >>>",String.valueOf(3));
                    SHADE_AMOUNT = 10;
            }

            Log.d("SHADE AMOUNT >>> ",String.valueOf(shadeAmount));

            resources[0] = R.drawable.island_1;
            resources[1] = R.drawable.island_2;
            resources[2] = R.drawable.island_3;
            resources[3] = R.drawable.island_4;
            resources[4] = R.drawable.island_5;

            cloud_resources[0] = R.drawable.cloud_1;
            cloud_resources[1] = R.drawable.cloud_2;
            cloud_resources[2] = R.drawable.cloud_3;
            cloud_resources[3] = R.drawable.cloud_4;
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            islands = new ArrayList<Island>();
            clouds = new ArrayList<Cloud>();
            //Load in our maps from res drawable
            islands.add(new Island(BitmapFactory.decodeResource(getResources(), resources[(int) Math.round(Math.random() * (resources.length - 1))])));
            clouds.add(new Cloud(BitmapFactory.decodeResource(getResources(), cloud_resources[(int) Math.round(Math.random() * (cloud_resources.length - 1))])));

            Log.d("ONCREATE >>>> ", "CREATED");
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            Log.d("SURFACE CREATE >>>> ", "CREATED");
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(PinballRunnable);
            Log.d("SURFACE DESTROYED >>>> ", "DESTROYED");
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder,
                                     int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            drawFrame();
        }

        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();
            Matrix mat = new Matrix();
            Canvas canvas = null;
            Paint p = new Paint();
            Paint pixelPerfect = new Paint(Paint.FILTER_BITMAP_FLAG);

            p.setColor(SEA_COLOR);
            p.setStyle(Paint.Style.FILL);

            pixelPerfect.setAntiAlias(false);
            pixelPerfect.setDither(false);
            pixelPerfect.setFilterBitmap(false);
            mat.setScale(SCALE, SCALE);

            try {
                canvas = holder.lockCanvas();

                if(canvas != null) {
                    canvas.setMatrix(null);
                    //Erase what was on the screen
                    canvas.drawRect(0, 0, WallWidth, WallHeight, p);
                    canvas.setMatrix(mat);

                    for(Island i : islands) {
                        canvas.drawBitmap(i.getImage(),i.x, i.y, pixelPerfect);
                        i.MoveDown(PAN_SPEED);
                    }

                    for(Cloud c : clouds) {
                        if(TRANSPARENT_CLOUDS) {
                            pixelPerfect.setAlpha(100);
                        }

                        canvas.drawBitmap(c.getImage(), c.x, c.y, pixelPerfect);
                        c.MoveDown(PAN_SPEED);
                        pixelPerfect.setAlpha(255);
                    }

                    canvas.setMatrix(null);
                    drawShade(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            handler.removeCallbacks(PinballRunnable);
            handler.postDelayed(PinballRunnable, 1000 / framerate);
        }

        private void drawShade(Canvas can) {
            if (can != null) {
                Paint shadeP = new Paint();
                shadeP.setColor(Color.BLACK);
                shadeP.setAlpha(SHADE_AMOUNT);
                can.drawRect(0,0, WallWidth, WallHeight, shadeP);
            }
        }
    }

    //Class that holds information for each map item that will scroll down the screen
    class MapItem {
        private int MapHeight = 640;
        public float x = 0, y = 0;
        private Bitmap image = null;
        private boolean hasAddedIsland = false;

        public MapItem(Bitmap source) {
            this.image = source;
            this.y = -source.getHeight();
            this.x = source.getWidth() / -2;
        }

        public void MoveDown(int speed) {
            this.y += speed / 60;

            if(this.y > (WallHeight / 2) && !hasAddedIsland) {
                int val = (int) Math.round(Math.random() * (resources.length - 1));
                islands.add(new Island(BitmapFactory.decodeResource(getResources(), resources[val])));
                hasAddedIsland = true;
            }
        }

        public Bitmap getImage(){
            return this.image;
        }
    }

    class Island extends MapItem {

        public Island(Bitmap source) {
            super(source);
        }
    }

    class Cloud extends MapItem {

        private boolean hasAddedCloud = false;
        public Cloud(Bitmap source) {
            super(source);

            this.x = 0;
        }

        public void MoveDown(int speed) {
            this.y += (speed / 60) / 4;

            if(this.y > (WallHeight / 2) && !hasAddedCloud) {
                int val = (int) Math.round(Math.random() * (cloud_resources.length - 1));
                clouds.add(new Cloud(BitmapFactory.decodeResource(getResources(), cloud_resources[val])));
                hasAddedCloud = true;
            }
        }
    }
}