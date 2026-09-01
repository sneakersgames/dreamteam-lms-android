# The web game reaches native code through this object. R8 cannot see the call sites, so
# without this the bridge method is renamed or removed and every request goes unanswered,
# leaving the web app permanently in an anonymous session.
-keepclassmembers class com.engagecraft.gaming.epllastmanstanding.web.GhBridge {
    @android.webkit.JavascriptInterface <methods>;
}
