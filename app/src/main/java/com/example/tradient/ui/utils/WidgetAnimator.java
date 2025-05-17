package com.example.tradient.ui.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.transition.TransitionManager;
import android.transition.ChangeBounds;
import android.transition.ChangeTransform;
import android.transition.ChangeClipBounds;
import android.transition.ChangeImageTransform;
import android.transition.TransitionSet;

import androidx.annotation.NonNull;

/**
 * Utility class for animations and transitions in the Tradient app.
 */
public class WidgetAnimator {

    /**
     * Applies touch feedback animations to a view.
     * 
     * @param view The view to apply animations to
     */
    public static void applyTouchFeedback(@NonNull final View view) {
        // Create press animation programmatically
        AnimationSet pressAnim = new AnimationSet(true);
        pressAnim.setDuration(200);
        
        ScaleAnimation pressScale = new ScaleAnimation(
                1.0f, 0.95f, 1.0f, 0.95f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        pressScale.setDuration(200);
        
        AlphaAnimation pressAlpha = new AlphaAnimation(1.0f, 0.9f);
        pressAlpha.setDuration(200);
        
        pressAnim.addAnimation(pressScale);
        pressAnim.addAnimation(pressAlpha);
        
        // Create release animation programmatically
        AnimationSet releaseAnim = new AnimationSet(true);
        releaseAnim.setDuration(200);
        
        ScaleAnimation releaseScale = new ScaleAnimation(
                0.95f, 1.0f, 0.95f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        releaseScale.setDuration(200);
        
        AlphaAnimation releaseAlpha = new AlphaAnimation(0.9f, 1.0f);
        releaseAlpha.setDuration(200);
        
        releaseAnim.addAnimation(releaseScale);
        releaseAnim.addAnimation(releaseAlpha);
        
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.startAnimation(pressAnim);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.startAnimation(releaseAnim);
                    // Perform click after animation completes
                    new Handler().postDelayed(() -> view.performClick(), 100);
                    return true;
            }
            return false;
        });
    }
    
    /**
     * Applies smooth transitions when properties of a view change.
     * 
     * @param container The ViewGroup containing the views to animate
     * @param duration Duration in milliseconds for the transition
     */
    public static void applyPropertyTransition(@NonNull ViewGroup container, long duration) {
        // Create transition set programmatically
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(new ChangeBounds());
        transitionSet.addTransition(new ChangeTransform());
        transitionSet.addTransition(new ChangeClipBounds());
        transitionSet.addTransition(new ChangeImageTransform());
        transitionSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
        transitionSet.setDuration(duration);
        
        TransitionManager.beginDelayedTransition(container, transitionSet);
    }
    
    /**
     * Temporarily disables a view while an operation completes.
     * 
     * @param view The view to disable
     * @param durationMs How long to disable the view in milliseconds
     */
    public static void temporarilyDisable(@NonNull View view, long durationMs) {
        view.setEnabled(false);
        view.animate()
                .alpha(0.5f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        new Handler().postDelayed(() -> {
                            view.animate()
                                    .alpha(1.0f)
                                    .setDuration(100)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            view.setEnabled(true);
                                        }
                                    });
                        }, durationMs);
                    }
                });
    }
} 