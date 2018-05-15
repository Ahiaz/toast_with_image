package nl.xservices.plugins;

import android.util.Log;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.view.animation.AlphaAnimation;
import android.animation.ObjectAnimator;
import android.view.animation.Animation;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Context;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.net.URL;
import java.io.InputStream;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.util.DisplayMetrics;
import android.view.Display;
import android.os.AsyncTask;
import com.squareup.picasso.Picasso;
import android.view.animation.LinearInterpolator;
import com.daasuu.ei.Ease;
import com.daasuu.ei.EasingInterpolator;

public class Toast extends CordovaPlugin {


  public RotateAnimation animRotate = null;

  public AlphaAnimation animFade = null;

  public ObjectAnimator linearX = null;

  private static final String ACTION_SHOW_EVENT = "show";
  private static final String ACTION_HIDE_EVENT = "hide";
  private static final String ACTION_SHOW_IMAGE_EVENT = "showWithImage";


  private static final int GRAVITY_TOP = Gravity.TOP|Gravity.CENTER_HORIZONTAL;
  private static final int GRAVITY_CENTER = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
  private static final int GRAVITY_BOTTOM = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;

  private static final int BASE_TOP_BOTTOM_OFFSET = 20;

  private android.widget.Toast mostRecentToast;
  private ViewGroup viewGroup;

  private static final boolean IS_AT_LEAST_LOLLIPOP = Build.VERSION.SDK_INT >= 21;

  // note that webView.isPaused() is not Xwalk compatible, so tracking it poor-man style
  private boolean isPaused;

  private String currentMessage;
  private JSONObject currentData;
  private static CountDownTimer _timer;

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {


    if (ACTION_SHOW_IMAGE_EVENT.equals(action)) { //Image option


      if(args.getString(5).equals("resource")){


      showWithImage(args.getString(0), args.getString(1), args.getInt(2), args.getInt(3), args.getInt(4), args.getString(5), args.getInt(6), args.getString(7), callbackContext); // callback or value

      }

      else{ //from url


      showWithImageFromUrl(args.getString(0), args.getString(1), args.getInt(2), args.getInt(3), args.getInt(4), args.getString(5), args.getInt(6), args.getString(7), callbackContext); // callback or value


      }
      callbackContext.success();

      return true;


    }


    if (ACTION_HIDE_EVENT.equals(action)) {
      returnTapEvent("hide", currentMessage, currentData, callbackContext);
      hide();
      callbackContext.success();
      return true;

    } else if (ACTION_SHOW_EVENT.equals(action)) {
      if (this.isPaused) {
        return true;
      }

      final JSONObject options = args.getJSONObject(0);
      final String msg = options.getString("message");
      final Spannable message = new SpannableString(msg);
      message.setSpan(
          new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
          0,
          msg.length() - 1,
          Spannable.SPAN_INCLUSIVE_INCLUSIVE);

      final String duration = options.getString("duration");
      final String position = options.getString("position");
      final int addPixelsY = options.has("addPixelsY") ? options.getInt("addPixelsY") : 0;
      final JSONObject data = options.has("data") ? options.getJSONObject("data") : null;
      final JSONObject styling = options.optJSONObject("styling");

      currentMessage = msg;
      currentData = data;

      cordova.getActivity().runOnUiThread(new Runnable() {
        public void run() {
          int hideAfterMs;
          if ("short".equalsIgnoreCase(duration)) {
            hideAfterMs = 2000;
          } else if ("long".equalsIgnoreCase(duration)) {
            hideAfterMs = 4000;
          } else {
            // assuming a number of ms
            hideAfterMs = Integer.parseInt(duration);
          }
          final android.widget.Toast toast = android.widget.Toast.makeText(
              IS_AT_LEAST_LOLLIPOP ? cordova.getActivity().getWindow().getContext() : cordova.getActivity().getApplicationContext(),
              message,
              android.widget.Toast.LENGTH_LONG // actually controlled by a timer further down
          );

          if ("top".equals(position)) {
            toast.setGravity(GRAVITY_TOP, 0, BASE_TOP_BOTTOM_OFFSET + addPixelsY);
          } else  if ("bottom".equals(position)) {
            toast.setGravity(GRAVITY_BOTTOM, 0, BASE_TOP_BOTTOM_OFFSET - addPixelsY);
          } else if ("center".equals(position)) {
            toast.setGravity(GRAVITY_CENTER, 0, addPixelsY);
          } else {
            callbackContext.error("invalid position. valid options are 'top', 'center' and 'bottom'");
            return;
          }

          // if one of the custom layout options have been passed in, draw our own shape
          if (styling != null && Build.VERSION.SDK_INT >= 16) {

            // the defaults mimic the default toast as close as possible
            final String backgroundColor = styling.optString("backgroundColor", "#333333");
            final String textColor = styling.optString("textColor", "#ffffff");
            final Double textSize = styling.optDouble("textSize", -1);
            final double opacity = styling.optDouble("opacity", 0.8);
            final int cornerRadius = styling.optInt("cornerRadius", 100);
            final int horizontalPadding = styling.optInt("horizontalPadding", 50);
            final int verticalPadding = styling.optInt("verticalPadding", 30);

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(cornerRadius);
            shape.setAlpha((int)(opacity * 255)); // 0-255, where 0 is an invisible background
            shape.setColor(Color.parseColor(backgroundColor));
            toast.getView().setBackground(shape);

            final TextView toastTextView;
            toastTextView = (TextView) toast.getView().findViewById(android.R.id.message);
            toastTextView.setTextColor(Color.parseColor(textColor));
            if (textSize > -1) {
              toastTextView.setTextSize(textSize.floatValue());
            }

            toast.getView().setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

            // this gives the toast a very subtle shadow on newer devices
            if (Build.VERSION.SDK_INT >= 21) {
              toast.getView().setElevation(6);
            }
          }

          // On Android >= 5 you can no longer rely on the 'toast.getView().setOnTouchListener',
          // so created something funky that compares the Toast position to the tap coordinates.
          if (IS_AT_LEAST_LOLLIPOP) {
            getViewGroup().setOnTouchListener(new View.OnTouchListener() {
              @Override
              public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() != MotionEvent.ACTION_DOWN) {
                  return false;
                }
                if (mostRecentToast == null || !mostRecentToast.getView().isShown()) {
                  getViewGroup().setOnTouchListener(null);
                  return false;
                }

                float w = mostRecentToast.getView().getWidth();
                float startX = (view.getWidth() / 2) - (w / 2);
                float endX = (view.getWidth() / 2) + (w / 2);

                float startY;
                float endY;

                float g = mostRecentToast.getGravity();
                float y = mostRecentToast.getYOffset();
                float h = mostRecentToast.getView().getHeight();

                if (g == GRAVITY_BOTTOM) {
                  startY = view.getHeight() - y - h;
                  endY = view.getHeight() - y;
                } else if (g == GRAVITY_CENTER) {
                  startY = (view.getHeight() / 2) + y - (h / 2);
                  endY = (view.getHeight() / 2) + y + (h / 2);
                } else {
                  // top
                  startY = y;
                  endY = y + h;
                }

                float tapX = motionEvent.getX();
                float tapY = motionEvent.getY();

                final boolean tapped = tapX >= startX && tapX <= endX &&
                    tapY >= startY && tapY <= endY;

                return tapped && returnTapEvent("touch", msg, data, callbackContext);
              }
            });
          } else {
            toast.getView().setOnTouchListener(new View.OnTouchListener() {
              @Override
              public boolean onTouch(View view, MotionEvent motionEvent) {
                return motionEvent.getAction() == MotionEvent.ACTION_DOWN && returnTapEvent("touch", msg, data, callbackContext);
              }
            });
          }
          // trigger show every 2500 ms for as long as the requested duration
          _timer = new CountDownTimer(hideAfterMs, 2500) {
            public void onTick(long millisUntilFinished) {toast.show();}
            public void onFinish() {
              returnTapEvent("hide", msg, data, callbackContext);
              toast.cancel();
            }
          }.start();

          mostRecentToast = toast;
          toast.show();

          PluginResult pr = new PluginResult(PluginResult.Status.OK);
          pr.setKeepCallback(true);
          callbackContext.sendPluginResult(pr);
        }
      });

      return true;
    } else {
      callbackContext.error("toast." + action + " is not a supported function. Did you mean '" + ACTION_SHOW_EVENT + "'?");
      return false;
    }
  }


  private void hide() {
    if (mostRecentToast != null) {
      mostRecentToast.cancel();
      getViewGroup().setOnTouchListener(null);
    }
    if (_timer != null) {
      _timer.cancel();
    }
  }

  //SHOW WITH IMAGE



      private void showWithImageFromUrl( /*String message,*/ String url, String Toastposition, int duration, int screenWidth, int blinking, String from, int percentage, String animation, CallbackContext callbackContext) {

      try{


            cordova.getActivity().runOnUiThread(new Runnable() {
        public void run() {



          int TheGravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;

          switch(Toastposition){

            case "TOP": 

            TheGravity = Gravity.TOP|Gravity.CENTER_HORIZONTAL;

            break;

            case "BOTTOM":

            TheGravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;

            break;

          }

          //rotate animation

            animRotate = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            animRotate.setInterpolator(new LinearInterpolator());
            animRotate.setRepeatCount(Animation.INFINITE);
            animRotate.setDuration(duration);

          //Fade animation
          
          animFade = new AlphaAnimation(0.0f, 1.0f);
          animFade.setRepeatCount(Animation.INFINITE);
          animFade.setRepeatMode(Animation.REVERSE);
          animFade.setDuration(duration/4);  



            Context contextToast = IS_AT_LEAST_LOLLIPOP ? cordova.getActivity().getWindow().getContext() : cordova.getActivity().getApplicationContext();

            // Retrieve the resource
            int custom_layout = cordova.getActivity().getResources().getIdentifier("toast", "layout", cordova.getActivity().getPackageName());


             LayoutInflater inflater =  cordova.getActivity().getLayoutInflater();

             ViewGroup mylayout = (ViewGroup) cordova.getActivity().findViewById(cordova.getActivity().getResources().getIdentifier("toastLayout", "id", cordova.getActivity().getPackageName()));

            View toastView = inflater.inflate(custom_layout, mylayout);

                ImageView imageView = (ImageView)toastView.findViewById(cordova.getActivity().getResources().getIdentifier("imageView", "id", cordova.getActivity().getPackageName()));



                int width =  (screenWidth*percentage)/100; //% of screen width (square shape logo)

                //String img = "<html><head><head><style type='text/css'>body{margin:auto auto;text-align:center;} img{width:"+width+";height:"+width+"} </style></head><body><img src=\"" +url+ "\"></body></html>";



                //imageView.loadDataWithBaseURL(null, img, "html/css", "utf-8", null);


                //imageView.setImageResource(cordova.getActivity().getResources().getIdentifier(url, "drawable", cordova.getActivity().getPackageName()));


                
//
               //TextView textView = (TextView)toastView.findViewById(cordova.getActivity().getResources().getIdentifier("text", "id", cordova.getActivity().getPackageName()));

                //textView.setText(message);

                imageView.getLayoutParams().height = width;
                imageView.getLayoutParams().width = width;
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);//important to get a square image with same dimensions
                Picasso.get().load(url).into(imageView);

                android.widget.Toast toastImage = new android.widget.Toast(contextToast);

                toastImage.setGravity(TheGravity, 0, 0);
               // toastImage.setDuration(android.widget.Toast.LENGTH_LONG);
                toastImage.setView(toastView);


                          // trigger show every 3000 ms for as long as the requested duration
          _timer = new CountDownTimer(duration, blinking) {

            public void onTick(long millisUntilFinished) {toastImage.show(); 



            }
            public void onFinish() {
              toastImage.cancel();
              imageView.setAnimation(null);

            }
          };


                toastImage.show();

                  switch(animation){

                  case "rotate":
                  imageView.startAnimation(animRotate);
                  break;

                  case "fade":
                  imageView.startAnimation(animFade);
                  break;

                  case "linearx":

                linearX = ObjectAnimator.ofFloat(imageView, "translationX", 0, screenWidth/2, 0);
                linearX.setInterpolator(new EasingInterpolator(Ease.LINEAR));
                linearX.setStartDelay(0);
                linearX.setDuration(duration/4);
                linear.start();
                  break;

                  default:

                  imageView.startAnimation(animRotate);
                  break;


                }

                _timer.start();

                mostRecentToast = toastImage;


          PluginResult pr = new PluginResult(PluginResult.Status.OK);
          pr.setKeepCallback(true);
          callbackContext.sendPluginResult(pr);
               
          callbackContext.success("complete");

        
 
        }
      });

}catch(Exception e){callbackContext.error(e.toString()); Log.i("exception", e.toString());}

  }


    private void showWithImage( /*String message,*/ String url, String Toastposition, int duration, int screenWidth, int blinking, String from, int percentage, String animation, CallbackContext callbackContext) {

      try{



            cordova.getActivity().runOnUiThread(new Runnable() {
        public void run() {



          int TheGravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;

          switch(Toastposition){

            case "TOP": 

            TheGravity = Gravity.TOP|Gravity.CENTER_HORIZONTAL;

            break;

            case "BOTTOM":

            TheGravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;

            break;

          }

          //rotate animation

            animRotate = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            animRotate.setInterpolator(new LinearInterpolator());
            animRotate.setRepeatCount(Animation.INFINITE);
            animRotate.setDuration(duration);


           //Fade animation
          
          animFade = new AlphaAnimation(0.0f, 1.0f);
          animFade.setRepeatCount(Animation.INFINITE);
          animFade.setRepeatMode(Animation.REVERSE);
          animFade.setDuration(1000); 



            Context contextToast = IS_AT_LEAST_LOLLIPOP ? cordova.getActivity().getWindow().getContext() : cordova.getActivity().getApplicationContext();

            // Retrieve the resource
            int custom_layout = cordova.getActivity().getResources().getIdentifier("toast", "layout", cordova.getActivity().getPackageName());


             LayoutInflater inflater =  cordova.getActivity().getLayoutInflater();

             ViewGroup mylayout = (ViewGroup) cordova.getActivity().findViewById(cordova.getActivity().getResources().getIdentifier("toastLayout", "id", cordova.getActivity().getPackageName()));

            View toastView = inflater.inflate(custom_layout, mylayout);

                ImageView imageView = (ImageView)toastView.findViewById(cordova.getActivity().getResources().getIdentifier("imageView", "id", cordova.getActivity().getPackageName()));

               // imageView.setBackgroundColor(Color.TRANSPARENT);

                //set image width and height responsive

               // android.view.Display display = getWindowManager().getDefaultDisplay();

                int width =  (screenWidth*percentage)/100; //% of screen width (square shape logo)

                //String img = "<html><head><head><style type='text/css'>body{margin:auto auto;text-align:center;} img{width:"+width+";height:"+width+"} </style></head><body><img src=\"" +url+ "\"></body></html>";



                //imageView.loadDataWithBaseURL(null, img, "html/css", "utf-8", null);
          

                imageView.setImageResource(cordova.getActivity().getResources().getIdentifier(url, "drawable", cordova.getActivity().getPackageName()));


                
//
               //TextView textView = (TextView)toastView.findViewById(cordova.getActivity().getResources().getIdentifier("text", "id", cordova.getActivity().getPackageName()));

                //textView.setText(message);

              imageView.getLayoutParams().height = width;
                imageView.getLayoutParams().width = width;
               imageView.setScaleType(ImageView.ScaleType.FIT_XY); //important to get a square image with same dimensions


                android.widget.Toast toastImage = new android.widget.Toast(contextToast);

                toastImage.setGravity(TheGravity, 0, 0);
               // toastImage.setDuration(android.widget.Toast.LENGTH_LONG);
                toastImage.setView(toastView);


                          // trigger show every 3000 ms for as long as the requested duration
          _timer = new CountDownTimer(duration, blinking) {

            public void onTick(long millisUntilFinished) {


              toastImage.show(); 



            }
            public void onFinish() {
              toastImage.cancel();
              imageView.setAnimation(null);
            }
          };


                toastImage.show();


                  switch(animation){

                  case "rotate":
                  imageView.startAnimation(animRotate);
                  break;

                  case "fade":
                  imageView.startAnimation(animFade);
                  break;


                  case "linearx":
                linearX = ObjectAnimator.ofFloat(imageView, "translationX", 0, screenWidth/2, 0); //first position, end, back to start
                linearX.setInterpolator(new EasingInterpolator(Ease.LINEAR));
                linearX.setStartDelay(0);
                linearX.setDuration(duration/4);
                  linear.start();
                  break;

                  default:

                  imageView.startAnimation(animRotate);
                  break;

                }

                _timer.start();

                mostRecentToast = toastImage;


          PluginResult pr = new PluginResult(PluginResult.Status.OK);
          pr.setKeepCallback(true);
          callbackContext.sendPluginResult(pr);
               
          callbackContext.success("complete");

        
 
        }
      });

}catch(Exception e){callbackContext.error(e.toString()); Log.i("lleguee", e.toString());}

  }




  private boolean returnTapEvent(String eventName, String message, JSONObject data, CallbackContext callbackContext) {
    final JSONObject json = new JSONObject();
    try {
      json.put("event", eventName);
      json.put("message", message);
      json.put("data", data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    callbackContext.success(json);
    return true;
  }

  // lazy init and caching
  private ViewGroup getViewGroup() {
    if (viewGroup == null) {
      viewGroup = (ViewGroup) ((ViewGroup) cordova.getActivity().findViewById(android.R.id.content)).getChildAt(0);
    }
    return viewGroup;
  }

  @Override
  public void onPause(boolean multitasking) {
    hide();
    this.isPaused = true;
  }

  @Override
  public void onResume(boolean multitasking) {
    this.isPaused = false;
  }
} //here

    