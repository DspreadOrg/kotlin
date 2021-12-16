package com.dspread.demoui.activities

import android.annotation.TargetApi
import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.menu.ActionMenuItemView
import android.support.v7.widget.ActionMenuView
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import com.dspread.demoui.R
import java.util.*

/**
 * BaseActivity used for to build all activity
 * @author Qianmeng
 */
abstract class BaseActivity : AppCompatActivity() {
    private var toolbar: Toolbar? = null
    private var txt_toolbar_title: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        var savedInstanceState = savedInstanceState
        if (savedInstanceState != null) {
            savedInstanceState.clear()
            savedInstanceState = null
        }
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        toolbar = findViewById(R.id.toolbar)
        if (toolbar != null) {
            txt_toolbar_title = toolbar!!.findViewById(R.id.txt_toolbar_title)
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true) //show the left arrow
            supportActionBar!!.setDisplayShowTitleEnabled(false)
            setDefaultToolbarColor()
            toolbar!!.setPadding(0, 0, 0, 0)
        }
        toolbar!!.setNavigationOnClickListener(View.OnClickListener { onToolbarLinstener() })
    }

    fun getToolbar(): Toolbar? {
        return if (toolbar != null) {
            toolbar
        } else null
    }

    abstract fun onToolbarLinstener()
    protected abstract val layoutId: Int

    //protected abstract int getFragmentContainer();
    fun setActionBarIcon(iconRes: Int) {
        toolbar!!.setNavigationIcon(iconRes)
    }

    override fun setTitle(titleResource: Int) {
        setTitle(resources.getString(titleResource))
    }

    fun setTitle(title: String?) {
        if (title != null && title != "") {
            if (txt_toolbar_title != null) {
                txt_toolbar_title!!.text = title
                toolbar!!.title = ""
            } else if (toolbar != null) {
                toolbar!!.title = title
                txt_toolbar_title!!.text = ""
            }
        }
    }

    fun setDefaultToolbarColor() {
        setToolbarBgColor(ContextCompat.getColor(this, R.color.eb_col_11))
        //        setToolbarTextColor(ContextCompat.getColor(this,R.color.eb_col_30));
        setToolbarIconColor(ContextCompat.getColor(this, R.color.eb_col_30))
        setStatusBarColor(ContextCompat.getColor(this, R.color.eb_col_11))
    }

    fun setCustomToolbarColor(strCustomColor: String?) {
        if (strCustomColor != null) {
            val customColor = Color.parseColor(strCustomColor)
            // set toolbar bg color
            setToolbarBgColor(customColor)
            // set toolbar text color
            val midColor = resources.getColor(R.color.custom_middle_color)
            if (customColor >= midColor) {
                //                setToolbarTextColor(getResources().getColor(R.color.custom_dark_color));
                setToolbarIconColor(resources.getColor(R.color.custom_dark_color))
            } else {
                //                setToolbarTextColor(getResources().getColor(R.color.custom_light_color));
                setToolbarIconColor(resources.getColor(R.color.custom_light_color))
            }
            setStatusBarColor(customColor)
        }
    }

    fun setToolbarBgColor(color: Int) {
        if (toolbar != null) {
            toolbar!!.setBackgroundColor(color)
        }
    }

    fun setToolbarTextColor(color: Int) {
        if (toolbar != null) {
            // change title text color
            toolbar!!.setTitleTextColor(color)
            // change toolbar background color
            val upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material)
            upArrow!!.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        }
    }

    @TargetApi(21)
    fun setStatusBarColor(color: Int) {
        if (isAboveKITKAT) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            //            window.setStatusBarColor(color);
        }
    }

    val statusBarHeight: Int
        get() {
            var result = 0
            if (isAboveKITKAT) {
                val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceId > 0) {
                    result = resources.getDimensionPixelSize(resourceId)
                }
            }
            return result
        }
    val isAboveKITKAT: Boolean
        get() {
            var isHigher = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                isHigher = true
            }
            return isHigher
        }

    /**
     * Use this method to colorize toolbar icons to the desired target color
     *
     * @param toolbarIconsColor the target color of toolbar icons
     */
    fun setToolbarIconColor(toolbarIconsColor: Int) {
        if (toolbar != null) {
            val colorFilter = PorterDuffColorFilter(toolbarIconsColor, PorterDuff.Mode.SRC_ATOP) //MULTIPLY
            for (i in 0 until toolbar!!.childCount) {
                val v = toolbar!!.getChildAt(i)

                //Step 1 : Changing the color of back button (or open drawer button).
                if (v is ImageButton) {
                    //Action Bar back button
                    v.drawable.colorFilter = colorFilter
                }
                if (v is ActionMenuView) {
                    for (j in 0 until v.childCount) {
                        //Step 2: Changing the color of any ActionMenuViews - icons that
                        //are not back button, nor text, nor overflow menu icon.
                        val innerView = v.getChildAt(j)
                        if (innerView is ActionMenuItemView) {
                            val drawablesCount = innerView.compoundDrawables.size
                            for (k in 0 until drawablesCount) {
                                if (innerView.compoundDrawables[k] != null) {

                                    //Important to set the color filter in seperate thread,
                                    //by adding it to the message queue
                                    //Won't work otherwise.
                                    innerView.post { innerView.compoundDrawables[k].colorFilter = colorFilter }
                                }
                            }
                        }
                    }
                }

                //Step 3: Changing the color of title and subtitle.
                if (txt_toolbar_title != null) {
                    txt_toolbar_title!!.setTextColor(toolbarIconsColor)
                }
                toolbar!!.setTitleTextColor(toolbarIconsColor)
                toolbar!!.setSubtitleTextColor(toolbarIconsColor)

                //Step 4: Changing the color of the Overflow Menu icon.
                setOverflowButtonColor(this, colorFilter)
            }
        }
    }

    private fun setOverflowButtonColor(activity: Activity, colorFilter: PorterDuffColorFilter) {
        val overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description)
        val decorView = activity.window.decorView as ViewGroup
        val viewTreeObserver = decorView.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val outViews = ArrayList<View>()
                decorView.findViewsWithText(outViews, overflowDescription,
                        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
                if (outViews.isEmpty()) {
                    return
                }
                val overflow = outViews[0] as AppCompatImageView
                overflow.colorFilter = colorFilter
                removeOnGlobalLayoutListener(decorView, this)
            }
        })
    }

    private fun removeOnGlobalLayoutListener(v: View, listener: OnGlobalLayoutListener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            v.viewTreeObserver.removeGlobalOnLayoutListener(listener)
        } else {
            v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    companion object {
        const val TAG = "BaseActivity"
    }
}