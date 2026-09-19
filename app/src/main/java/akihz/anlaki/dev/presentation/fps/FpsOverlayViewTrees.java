package akihz.anlaki.dev.presentation.fps;

import android.view.View;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

/**
 * Installs composition owners on the overlay view.
 *
 * Java bridge: the three ViewTree owner classes resolve from javac but
 * not from kotlinc in this build, so the calls live here. Behavior is
 * identical to calling the setters from Kotlin.
 */
final class FpsOverlayViewTrees {

    private FpsOverlayViewTrees() {}

    static void install(
            View view,
            LifecycleOwner lifecycleOwner,
            ViewModelStoreOwner storeOwner,
            SavedStateRegistryOwner savedStateOwner) {
        ViewTreeLifecycleOwner.set(view, lifecycleOwner);
        ViewTreeViewModelStoreOwner.set(view, storeOwner);
        ViewTreeSavedStateRegistryOwner.set(view, savedStateOwner);
    }
}
