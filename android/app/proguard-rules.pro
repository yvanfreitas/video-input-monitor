# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/android/sdk/tools/proguard/proguard-android-optimize.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with HTML files,
# uncomment the following lines:
#-keepclassmembers class fqn.of.javascript.interface.for.webview {
#   public *;
#}
#-keepclassmembers class * extends android.webkit.WebViewClient {
#    public void onPageStarted(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
#    public void onPageFinished(android.webkit.WebView, java.lang.String);
#}
#-keepclassmembers class * extends android.webkit.WebChromeClient {
#    public boolean onConsoleMessage(android.webkit.ConsoleMessage);
#}
