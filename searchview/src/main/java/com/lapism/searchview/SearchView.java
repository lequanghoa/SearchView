package com.lapism.searchview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


@SuppressWarnings({"WeakerAccess", "unused"})
public class SearchView extends FrameLayout implements View.OnClickListener {

    public static final int ANIMATION_DURATION = 300;
    public static final int VERSION_TOOLBAR = 1000;
    public static final int VERSION_TOOLBAR_ICON = 1001;
    public static final int VERSION_MENU_ITEM = 1002;
    public static final int VERSION_MARGINS_TOOLBAR_SMALL = 2000;
    public static final int VERSION_MARGINS_TOOLBAR_BIG = 2001;
    public static final int VERSION_MARGINS_MENU_ITEM = 2002;
    public static final int THEME_LIGHT = 3000;
    public static final int THEME_DARK = 3001;
    public static final int SPEECH_REQUEST_CODE = 4000;

    private static int mIconColor = Color.BLACK;
    private static int mTextColor = Color.BLACK;
    private static int mTextHighlightColor = Color.BLACK;
    private static int mTextStyle = Typeface.NORMAL;
    private static Typeface mTextFont = Typeface.DEFAULT;
    private static CharSequence mUserQuery = "";

    private final Context mContext;

    protected OnQueryTextListener mOnQueryChangeListener = null;
    protected OnOpenCloseListener mOnOpenCloseListener = null;
    protected OnMenuClickListener mOnMenuClickListener = null;
    protected SearchArrowDrawable mSearchArrow = null;

    protected View mShadowView;
    protected CardView mCardView;
    protected SearchEditText mEditText;
    protected ImageView mBackImageView;
    protected ImageView mEmptyImageView;

    protected int mVersion = VERSION_TOOLBAR;
    protected int mAnimationDuration = ANIMATION_DURATION;
    protected float mIsSearchArrowHamburgerState = SearchArrowDrawable.STATE_HAMBURGER;
    protected boolean mShadow = true;
    protected boolean mIsSearchOpen = false;
    protected CharSequence mOldQueryText;

    private boolean mShouldClearOnClose = false;
    private boolean mShouldClearOnOpen = false;

    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();
        initStyle(attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SearchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        initView();
        initStyle(attrs, defStyleAttr);
    }

    public static int getIconColor() {
        return mIconColor;
    }

    public void setIconColor(@ColorInt int color) {
        mIconColor = color;

        ColorFilter colorFilter = new PorterDuffColorFilter(mIconColor, PorterDuff.Mode.SRC_IN);

        mBackImageView.setColorFilter(colorFilter);
        mEmptyImageView.setColorFilter(colorFilter);

        if (mSearchArrow != null) {
            mSearchArrow.setColorFilter(colorFilter);
        }
    }

    public static int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(@ColorInt int color) {
        mTextColor = color;
        mEditText.setTextColor(mTextColor);
    }

    public static int getTextHighlightColor() {
        return mTextHighlightColor;
    }

    public void setTextHighlightColor(@ColorInt int color) {
        mTextHighlightColor = color;
    }

    public static Typeface getTextFont() {
        return mTextFont;
    }

    public void setTextFont(Typeface font) {
        mTextFont = font;
        mEditText.setTypeface((Typeface.create(mTextFont, mTextStyle)));
    }

    public static int getTextStyle() {
        return mTextStyle;
    }

    public void setTextStyle(int style) {
        mTextStyle = style;
        mEditText.setTypeface((Typeface.create(mTextFont, mTextStyle)));
    }

    // ---------------------------------------------------------------------------------------------
    public static CharSequence getQuery() {
        return mUserQuery;
    }

    public void setQuery(CharSequence query) {
        setQueryWithoutSubmitting(query);

        if (!TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    private void setQueryWithoutSubmitting(CharSequence query) {
        mEditText.setText(query);
        if (query != null) {
            mEditText.setSelection(mEditText.length());
            mUserQuery = query;
        } else {
            mEditText.getText().clear();
        }
    }

    private void setQuery2(CharSequence query) {
        mEditText.setText(query);
        mEditText.setSelection(TextUtils.isEmpty(query) ? 0 : query.length());
    }

    // ---------------------------------------------------------------------------------------------
    private void initView() {
        LayoutInflater.from(mContext).inflate((R.layout.search_view), this, true);

        mCardView = (CardView) findViewById(R.id.cardView);

        mShadowView = findViewById(R.id.view_shadow);
        mShadowView.setOnClickListener(this);
        mShadowView.setVisibility(View.GONE);

        mBackImageView = (ImageView) findViewById(R.id.imageView_arrow_back);
        mBackImageView.setOnClickListener(this);

        mEmptyImageView = (ImageView) findViewById(R.id.imageView_clear);
        mEmptyImageView.setOnClickListener(this);
        mEmptyImageView.setVisibility(View.GONE);

        mEditText = (SearchEditText) findViewById(R.id.searchEditText_input);
        mEditText.setSearchView(this);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                onSubmitQuery();
                return true;
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SearchView.this.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    addFocus();
                } else {
                    removeFocus();
                }
            }
        });

        setVersion(VERSION_TOOLBAR);
        setVersionMargins(VERSION_MARGINS_TOOLBAR_SMALL);
        setTheme(THEME_LIGHT, true);
    }

    private void initStyle(AttributeSet attrs, int defStyleAttr) {
        final TypedArray attr = mContext.obtainStyledAttributes(attrs, R.styleable.SearchView, defStyleAttr, 0);
        if (attr != null) {
            if (attr.hasValue(R.styleable.SearchView_search_version)) {
                setVersion(attr.getInt(R.styleable.SearchView_search_version, VERSION_TOOLBAR));
            }
            if (attr.hasValue(R.styleable.SearchView_search_version_margins)) {
                setVersionMargins(attr.getInt(R.styleable.SearchView_search_version_margins, VERSION_MARGINS_TOOLBAR_SMALL));
            }
            if (attr.hasValue(R.styleable.SearchView_search_theme)) {
                setTheme(attr.getInt(R.styleable.SearchView_search_theme, THEME_LIGHT), false);
            }
            if (attr.hasValue(R.styleable.SearchView_search_navigation_icon)) {
                setNavigationIcon(attr.getResourceId(R.styleable.SearchView_search_navigation_icon, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_icon_color)) {
                setIconColor(attr.getColor(R.styleable.SearchView_search_icon_color, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_background_color)) {
                setBackgroundColor(attr.getColor(R.styleable.SearchView_search_background_color, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_text)) {
                setText(attr.getString(R.styleable.SearchView_search_text));
            }
            if (attr.hasValue(R.styleable.SearchView_search_text_color)) {
                setTextColor(attr.getColor(R.styleable.SearchView_search_text_color, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_text_highlight_color)) {
                setTextHighlightColor(attr.getColor(R.styleable.SearchView_search_text_highlight_color, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_text_size)) {
                setTextSize(attr.getDimension(R.styleable.SearchView_search_text_size, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_text_style)) {
                setTextStyle(attr.getInt(R.styleable.SearchView_search_text_style, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_hint)) {
                setHint(attr.getString(R.styleable.SearchView_search_hint));
            }
            if (attr.hasValue(R.styleable.SearchView_search_hint_color)) {
                setHintColor(attr.getColor(R.styleable.SearchView_search_hint_color, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_animation_duration)) {
                setAnimationDuration(attr.getInt(R.styleable.SearchView_search_animation_duration, mAnimationDuration));
            }
            if (attr.hasValue(R.styleable.SearchView_search_shadow)) {
                setShadow(attr.getBoolean(R.styleable.SearchView_search_shadow, false));
            }
            if (attr.hasValue(R.styleable.SearchView_search_shadow_color)) {
                setShadowColor(attr.getColor(R.styleable.SearchView_search_shadow_color, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_elevation)) {
                setElevation(attr.getDimensionPixelSize(R.styleable.SearchView_search_elevation, 0));
            }
            if (attr.hasValue(R.styleable.SearchView_search_clear_on_close)) {
                setShouldClearOnClose(attr.getBoolean(R.styleable.SearchView_search_clear_on_close, true));
            }
            if (attr.hasValue(R.styleable.SearchView_search_clear_on_open)) {
                setShouldClearOnOpen(attr.getBoolean(R.styleable.SearchView_search_clear_on_open, true));
            }

            attr.recycle();
        }
    }

    // ---------------------------------------------------------------------------------------------
    public void setVersion(int version) {
        mVersion = version;

        if (mVersion == VERSION_TOOLBAR) {
            mEditText.clearFocus();
            mSearchArrow = new SearchArrowDrawable(mContext);

            mBackImageView.setImageDrawable(mSearchArrow);

            setVisibility(View.VISIBLE);
            mCardView.setVisibility(VISIBLE);
        }

        if (mVersion == VERSION_TOOLBAR_ICON) {
            mEditText.clearFocus();
            mBackImageView.setImageResource(R.drawable.search_ic_arrow_back_black_24dp);

            setVisibility(View.VISIBLE);
            mCardView.setVisibility(VISIBLE);
        }

        if (mVersion == VERSION_MENU_ITEM) {
            setVisibility(View.GONE);
            mBackImageView.setImageResource(R.drawable.search_ic_arrow_back_black_24dp);
        }

        mEmptyImageView.setImageResource(R.drawable.search_ic_clear_black_24dp);
    }

    public void setVersionMargins(int version) {
        CardView.LayoutParams params = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.WRAP_CONTENT
        );

        if (version == VERSION_MARGINS_TOOLBAR_SMALL) {
            int top = mContext.getResources().getDimensionPixelSize(R.dimen.search_toolbar_margin_top);
            int leftRight = mContext.getResources().getDimensionPixelSize(R.dimen.search_toolbar_margin_small_left_right);
            int bottom = 0;

            params.setMargins(leftRight, top, leftRight, bottom);
        } else if (version == VERSION_MARGINS_TOOLBAR_BIG) {
            int top = mContext.getResources().getDimensionPixelSize(R.dimen.search_toolbar_margin_top);
            int leftRight = mContext.getResources().getDimensionPixelSize(R.dimen.search_toolbar_margin_big_left_right);
            int bottom = 0;

            params.setMargins(leftRight, top, leftRight, bottom);
        } else if (version == VERSION_MARGINS_MENU_ITEM) {
            int margin = mContext.getResources().getDimensionPixelSize(R.dimen.search_menu_item_margin);

            params.setMargins(margin, margin, margin, margin);
        } else {
            params.setMargins(0, 0, 0, 0);
        }

        mCardView.setLayoutParams(params);
    }

    public void setTheme(int theme, boolean tint) {
        if (theme == THEME_LIGHT) {
            setBackgroundColor(ContextCompat.getColor(mContext, R.color.search_light_background));
            if (tint) {
                setIconColor(ContextCompat.getColor(mContext, R.color.search_light_icon));
                setHintColor(ContextCompat.getColor(mContext, R.color.search_light_hint));
                setTextColor(ContextCompat.getColor(mContext, R.color.search_light_text));
                setTextHighlightColor(ContextCompat.getColor(mContext, R.color.search_light_text_highlight));
            }
        }

        if (theme == THEME_DARK) {
            setBackgroundColor(ContextCompat.getColor(mContext, R.color.search_dark_background));
            if (tint) {
                setIconColor(ContextCompat.getColor(mContext, R.color.search_dark_icon));
                setHintColor(ContextCompat.getColor(mContext, R.color.search_dark_hint));
                setTextColor(ContextCompat.getColor(mContext, R.color.search_dark_text));
                setTextHighlightColor(ContextCompat.getColor(mContext, R.color.search_dark_text_highlight));
            }
        }
    }

    public void setNavigationIcon(int resource) {
        if (mVersion != VERSION_TOOLBAR) {
            mBackImageView.setImageResource(resource);
        }
    }

    public void setNavigationIcon(Drawable drawable) {
        if (drawable == null) {
            mBackImageView.setVisibility(View.GONE);
        } else if (mVersion != VERSION_TOOLBAR) {
            mBackImageView.setImageDrawable(drawable);
        }
    }

    @Override
    public void setBackgroundColor(@ColorInt int color) {
        mCardView.setCardBackgroundColor(color);
    }

    public void setText(CharSequence text) {
        mEditText.setText(text);
    }

    @SuppressWarnings("SameParameterValue")
    public void setText(@StringRes int text) {
        mEditText.setText(text);
    }

    public void setTextSize(float size) {
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setHint(CharSequence hint) {
        mEditText.setHint(hint);
    }

    @SuppressWarnings("SameParameterValue")
    public void setHint(@StringRes int hint) {
        mEditText.setHint(hint);
    }

    public void setHintColor(@ColorInt int color) {
        mEditText.setHintTextColor(color);
    }

    public void setAnimationDuration(int animationDuration) {
        mAnimationDuration = animationDuration;
    }

    public void setShadow(boolean shadow) {
        if (shadow) {
            mShadowView.setVisibility(View.VISIBLE);
        } else {
            mShadowView.setVisibility(View.GONE);
        }
        mShadow = shadow;
    }

    public void setShadowColor(@ColorInt int color) {
        mShadowView.setBackgroundColor(color);
    }

    @Override
    public void setElevation(float elevation) {
        mCardView.setMaxCardElevation(elevation);
        mCardView.setCardElevation(elevation);
        invalidate();
    }

    public boolean getShouldClearOnClose() {
        return mShouldClearOnClose;
    }

    public void setShouldClearOnClose(boolean shouldClearOnClose) {
        mShouldClearOnClose = shouldClearOnClose;
    }

    public boolean getShouldClearOnOpen() {
        return mShouldClearOnOpen;
    }

    public void setShouldClearOnOpen(boolean shouldClearOnOpen) {
        mShouldClearOnOpen = shouldClearOnOpen;
    }

    // ---------------------------------------------------------------------------------------------
    @SuppressWarnings("SameParameterValue")
    public void open(boolean animate) {
        if (mVersion == VERSION_MENU_ITEM) {
            setVisibility(View.VISIBLE);

            if (animate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    reveal();
                } else {
                    SearchAnimator.fadeOpen(mCardView, mAnimationDuration, mEditText, mShouldClearOnOpen, mOnOpenCloseListener);
                }
            } else {
                mCardView.setVisibility(View.VISIBLE);
                if (mShouldClearOnOpen && mEditText.length() > 0) {
                    mEditText.getText().clear();
                }
                mEditText.requestFocus();
                if (mOnOpenCloseListener != null) {
                    mOnOpenCloseListener.onOpen();
                }
            }
        }
        if (mVersion == VERSION_TOOLBAR) {
            if (mShouldClearOnOpen && mEditText.length() > 0) {
                mEditText.getText().clear();
            }
            mEditText.requestFocus();
        }
        if (mVersion == VERSION_TOOLBAR_ICON) {
            mEditText.requestFocus();
        }
    }

    @SuppressWarnings("SameParameterValue")
    public void close(boolean animate) {
        if (mVersion == VERSION_MENU_ITEM) {
            if (animate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    SearchAnimator.revealClose(mCardView, mAnimationDuration, mContext, mEditText, mShouldClearOnClose, this, mOnOpenCloseListener);
                } else {
                    SearchAnimator.fadeClose(mCardView, mAnimationDuration, mEditText, mShouldClearOnClose, this, mOnOpenCloseListener);
                }
            } else {
                if (mShouldClearOnClose && mEditText.length() > 0) {
                    mEditText.getText().clear();
                }
                mEditText.clearFocus();
                mCardView.setVisibility(View.GONE);
                setVisibility(View.GONE);
                if (mOnOpenCloseListener != null) {
                    mOnOpenCloseListener.onClose();
                }
            }
        }
        if (mVersion == VERSION_TOOLBAR) {
            if (mShouldClearOnClose && mEditText.length() > 0) {
                mEditText.getText().clear();
            }
            mEditText.clearFocus();
        }
        if (mVersion == VERSION_TOOLBAR_ICON) {
            mEditText.clearFocus();
        }
    }

    public void addFocus() {
        mIsSearchOpen = true;
        setArrow();
        if (mShadow) {
            SearchAnimator.fadeIn(mShadowView, mAnimationDuration);
        }
        showKeyboard();
        showClearTextIcon();
        if (mVersion != VERSION_MENU_ITEM) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mOnOpenCloseListener != null) {
                        mOnOpenCloseListener.onOpen();
                    }
                }
            }, mAnimationDuration);
        }
    }

    public void removeFocus() {
        mIsSearchOpen = false;
        if (mShadow) {
            SearchAnimator.fadeOut(mShadowView, mAnimationDuration);
        }
        if (mEditText.getText().length() == 0) {
            setHamburger();
        }
        hideKeyboard();
        mEmptyImageView.setVisibility(View.GONE);
        if (mVersion != VERSION_MENU_ITEM) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mOnOpenCloseListener != null) {
                        mOnOpenCloseListener.onClose();
                    }
                }
            }, mAnimationDuration);
        }
    }

    public boolean isSearchOpen() {
        return mIsSearchOpen;
    }

    // ---------------------------------------------------------------------------------------------
    private void onSubmitQuery() {
        CharSequence query = mEditText.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                mOnQueryChangeListener.onQueryTextSubmit(query.toString());
            }
        }
        mEditText.clearFocus();
    }

    private void onTextChanged(CharSequence newText) {
        CharSequence text = mEditText.getText();
        mUserQuery = text;
        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();

        if (!TextUtils.isEmpty(newText)) {
            showClearTextIcon();
        } else {
            hideClearTextIcon();
        }
    }

    public void showKeyboard() {
        if (!isInEditMode()) {
            InputMethodManager imm = (InputMethodManager) mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mEditText, 0);
            imm.showSoftInput(this, 0);
        }
    }

    public void hideKeyboard() {
        if (!isInEditMode()) {
            InputMethodManager imm = (InputMethodManager) mEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }
    }

    protected void setArrow() {
        if (mSearchArrow != null && mIsSearchArrowHamburgerState != SearchArrowDrawable.STATE_ARROW) {
            mSearchArrow.setVerticalMirror(false);
            mSearchArrow.animate(SearchArrowDrawable.STATE_ARROW, mAnimationDuration);
            mIsSearchArrowHamburgerState = SearchArrowDrawable.STATE_ARROW;
        }
    }

    private void setHamburger() {
        if (mSearchArrow != null && mIsSearchArrowHamburgerState != SearchArrowDrawable.STATE_HAMBURGER) {
            mSearchArrow.setVerticalMirror(true);
            mSearchArrow.animate(SearchArrowDrawable.STATE_HAMBURGER, mAnimationDuration);
            mIsSearchArrowHamburgerState = SearchArrowDrawable.STATE_HAMBURGER;
        }
    }

    private void hideClearTextIcon() {
        if (mUserQuery.length() == 0) {
            mEmptyImageView.setVisibility(View.GONE);
        }
    }

    private void showClearTextIcon() {
        if (mUserQuery.length() > 0) {
            mEmptyImageView.setVisibility(View.VISIBLE);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void reveal() {
        mCardView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mCardView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                SearchAnimator.revealOpen(mCardView, mAnimationDuration, mContext, mEditText, mShouldClearOnOpen, mOnOpenCloseListener);
            }
        });
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public void onClick(View v) {
        if (v == mBackImageView) {
            if (mVersion == VERSION_TOOLBAR) {
                if (mIsSearchArrowHamburgerState == SearchArrowDrawable.STATE_HAMBURGER) {
                    if (mOnMenuClickListener != null) {
                        mOnMenuClickListener.onMenuClick();
                    }
                }
                if (mIsSearchArrowHamburgerState == SearchArrowDrawable.STATE_ARROW) {
                    mEditText.getText().clear();
                    setHamburger();
                    close(true);
                }
            }
            if (mVersion == VERSION_TOOLBAR_ICON) {
                if (mOnMenuClickListener != null) {
                    mOnMenuClickListener.onMenuClick();
                }
            }
            if (mVersion == VERSION_MENU_ITEM) {
                close(true);
            }
        } else if (v == mEmptyImageView) {
            if (mEditText.length() > 0) {
                mEditText.getText().clear();
            }
        } else if (v == mShadowView) {
            close(true);
        }
    }

    // ---------------------------------------------------------------------------------------------
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    public void setOnOpenCloseListener(OnOpenCloseListener listener) {
        mOnOpenCloseListener = listener;
    }

    public void setOnMenuClickListener(OnMenuClickListener listener) {
        mOnMenuClickListener = listener;
    }

    // ---------------------------------------------------------------------------------------------

    public interface OnQueryTextListener {
        @SuppressWarnings({"UnusedParameters", "UnusedReturnValue", "SameReturnValue"})
        boolean onQueryTextChange(String newText);

        @SuppressWarnings("SameReturnValue")
        boolean onQueryTextSubmit(String query);
    }

    public interface OnOpenCloseListener {
        void onClose();

        void onOpen();
    }

    public interface OnMenuClickListener {
        void onMenuClick();
    }

    @Override protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.viewResultsState = !mEditText.hasFocus() && mEditText.getText().length() > 0;
        return savedState;
    }

    @Override protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        if (savedState.viewResultsState) {
            // basically all of the state of the view seems to be maintained fine without help
            // the only bad state it can get into is when the search has happened and the results are
            // being viewed, manually hack around this
            setArrow();
            mEmptyImageView.setVisibility(View.GONE);
        }
        super.onRestoreInstanceState(savedState.getSuperState());
    }

    private static class SavedState extends View.BaseSavedState {
        boolean viewResultsState;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.viewResultsState = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(viewResultsState ? 1 : 0);
        }

    }
}