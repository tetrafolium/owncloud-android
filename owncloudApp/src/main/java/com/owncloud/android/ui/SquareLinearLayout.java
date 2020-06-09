/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2016 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareLinearLayout extends LinearLayout {

public SquareLinearLayout(final Context context) {
	super(context);
}

public SquareLinearLayout(final Context context, final AttributeSet attrs) {
	super(context, attrs);
}

public SquareLinearLayout(final Context context, final AttributeSet attrs,
                          final int defStyle) {
	super(context, attrs, defStyle);
}

@Override
protected void onMeasure(final int widthMeasureSpec,
                         final int heightMeasureSpec) {
	super.onMeasure(widthMeasureSpec, widthMeasureSpec);
}
}
