package com.engagecraft.gaming.epllastmanstanding.web

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.engagecraft.gaming.core.lib.GamingConfig
import com.engagecraft.gaming.ui.shared.theme.core.GamingEnv

/**
 * Touch and scroll instrumentation for the game WebView, logged under [LOG_TAG].
 *
 * Gated at runtime rather than on `BuildConfig`: the published AAR is always the release variant,
 * so a build-type check would never switch this on inside a host app.
 */
internal object TouchDiagnostics {

    const val LOG_TAG = "ghscroll"

    /** On for any non-PROD env and for any debuggable host build. */
    fun isEnabled(context: Context): Boolean =
        GamingConfig.config.env != GamingEnv.PROD ||
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun log(message: String) {
        Log.d(LOG_TAG, message)
    }

    fun warn(message: String, throwable: Throwable? = null) {
        Log.w(LOG_TAG, message, throwable)
    }

    /**
     * Reports, through `console.log`, every non-passive touch/wheel listener registration, every
     * `preventDefault` on a touch/wheel event, a throttled trace of each gesture, and which
     * element actually scrolls. Must run before the app's own scripts to see their listeners.
     */
    val SCRIPT: String = """
        (function () {
          if (window.__ghScrollDiag) return;
          window.__ghScrollDiag = true;

          var TAG = '[$LOG_TAG]';
          var THROTTLE_MS = 250;

          function log() {
            try { console.log(TAG + ' ' + Array.prototype.join.call(arguments, ' ')); } catch (e) {}
          }

          function stack() {
            try {
              return (new Error().stack || '').split('\n').slice(3, 7)
                .map(function (l) { return l.trim(); }).join(' <- ');
            } catch (e) { return ''; }
          }

          function describe(el) {
            if (el === window) return 'window';
            if (el === document) return 'document';
            if (!el || !el.tagName) return String(el);
            var s = el.tagName.toLowerCase();
            if (el.id) s += '#' + el.id;
            var c = typeof el.className === 'string' ? el.className.trim() : '';
            if (c) s += '.' + c.split(/\s+/).slice(0, 3).join('.');
            return s;
          }

          function styleOf(el) {
            if (!el || el.nodeType !== 1) return '';
            var cs = getComputedStyle(el);
            return 'touch-action=' + cs.touchAction +
              ' overscroll-y=' + cs.overscrollBehaviorY +
              ' overflow-y=' + cs.overflowY;
          }

          var WATCHED = { touchstart: true, touchmove: true, wheel: true };

          var originalAdd = EventTarget.prototype.addEventListener;
          EventTarget.prototype.addEventListener = function (type, listener, options) {
            if (WATCHED[type]) {
              var passive = options !== null && typeof options === 'object' && 'passive' in options
                ? String(options.passive)
                : 'unset';
              if (passive !== 'true') {
                log('listener', type, 'passive=' + passive, 'on', describe(this), stack());
              }
            }
            return originalAdd.call(this, type, listener, options);
          };

          var originalPreventDefault = Event.prototype.preventDefault;
          Event.prototype.preventDefault = function () {
            if (this.type === 'wheel' || (this.type && this.type.indexOf('touch') === 0)) {
              log('preventDefault', this.type, 'cancelable=' + this.cancelable,
                'target=' + describe(this.target), stack());
            }
            return originalPreventDefault.apply(this, arguments);
          };

          var startY = 0;
          var startScrollY = 0;
          var moves = 0;
          var lastMoveLog = 0;
          var lastScrollLog = 0;
          var opts = { capture: true, passive: true };

          originalAdd.call(window, 'touchstart', function (e) {
            var t = e.touches[0];
            startY = t ? t.clientY : 0;
            startScrollY = window.scrollY;
            moves = 0;
            log('touchstart y=' + Math.round(startY), 'touches=' + e.touches.length,
              'scrollY=' + Math.round(window.scrollY), 'target=' + describe(e.target), styleOf(e.target));
          }, opts);

          originalAdd.call(window, 'touchmove', function (e) {
            moves++;
            var now = Date.now();
            if (now - lastMoveLog < THROTTLE_MS) return;
            lastMoveLog = now;
            var t = e.touches[0];
            log('touchmove dy=' + Math.round((t ? t.clientY : 0) - startY),
              'scrollY=' + Math.round(window.scrollY), 'cancelable=' + e.cancelable);
          }, opts);

          ['touchend', 'touchcancel'].forEach(function (type) {
            originalAdd.call(window, type, function (e) {
              log(type, 'moves=' + moves, 'scrolled=' + Math.round(window.scrollY - startScrollY),
                'defaultPrevented=' + e.defaultPrevented);
            }, opts);
          });

          originalAdd.call(document, 'scroll', function (e) {
            var now = Date.now();
            if (now - lastScrollLog < THROTTLE_MS) return;
            lastScrollLog = now;
            var el = e.target === document ? document.scrollingElement : e.target;
            log('scroll', describe(e.target), 'top=' + Math.round(el ? el.scrollTop : -1));
          }, opts);

          originalAdd.call(document, 'DOMContentLoaded', function () {
            var se = document.scrollingElement;
            log('layout viewport=' + window.innerHeight,
              'scrollHeight=' + (se ? se.scrollHeight : -1),
              'scrollingElement=' + describe(se));
            log('html', styleOf(document.documentElement));
            log('body', styleOf(document.body));
          });
        })();
    """.trimIndent()
}
