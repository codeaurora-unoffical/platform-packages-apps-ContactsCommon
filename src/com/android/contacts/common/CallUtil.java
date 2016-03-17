/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.contacts.common;

import com.android.contacts.common.util.PhoneNumberHelper;
import com.android.phone.common.PhoneConstants;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.os.SystemProperties;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.text.TextUtils;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Button;
import android.widget.TextView;

import com.android.contacts.common.R;

import java.util.List;

/**
 * Utilities related to calls that can be used by non system apps. These
 * use {@link Intent#ACTION_CALL} instead of ACTION_CALL_PRIVILEGED.
 *
 * The privileged version of this util exists inside Dialer.
 */
public class CallUtil {

    /*Enable Video calling irrespective of video capabilities*/
    public static final int ENABLE_VIDEO_CALLING = 1;
    /*Disable Video calling irrespective of video capabilities*/
    public static final int DISABLE_VIDEO_CALLING = 2;
    private static final int MAX_PHONE_NUM = 7;

    public static final String CONFIG_VIDEO_CALLING = "config_video_calling";
    public static final String DIALOG_VIDEO_CALLING = "display_video_call_dialog";
    private static AlertDialog mAlertDialog = null;

    /**
     * Return an Intent for making a phone call. Scheme (e.g. tel, sip) will be determined
     * automatically.
     */
    public static Intent getCallWithSubjectIntent(String number,
            PhoneAccountHandle phoneAccountHandle, String callSubject) {

        final Intent intent = getCallIntent(getCallUri(number));
        intent.putExtra(TelecomManager.EXTRA_CALL_SUBJECT, callSubject);
        if (phoneAccountHandle != null) {
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
        }
        return intent;
    }

    /**
     * Return an Intent for making a phone call. Scheme (e.g. tel, sip) will be determined
     * automatically.
     */
    public static Intent getCallIntent(String number) {
        return getCallIntent(number, null);
    }

    /**
     * Return an Intent for making a phone call. A given Uri will be used as is (without any
     * sanity check).
     */
    public static Intent getCallIntent(Uri uri) {
        if (PhoneNumberUtils.isEmergencyNumber(uri.getSchemeSpecificPart())) {
            return new Intent(Intent.ACTION_CALL_EMERGENCY, uri);
        } else {
            return new Intent(Intent.ACTION_CALL, uri);
        }
    }

    /**
     * A variant of {@link #getCallIntent(String, String)} but also include {@code Account}.
     */
    public static Intent getCallIntent(
            String number, PhoneAccountHandle accountHandle) {
        return getCallIntent(CallUtil.getCallUri(number), accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also accept a call
     * origin and {@code Account} and {@code VideoCallProfile} state.
     * For more information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(Uri uri, PhoneAccountHandle accountHandle) {
        final boolean isEmergency = PhoneNumberUtils.isEmergencyNumber(uri.getSchemeSpecificPart());
        final Intent intent = new Intent(isEmergency ? Intent.ACTION_CALL_EMERGENCY :
                Intent.ACTION_CALL, uri);
        if (accountHandle != null) {
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
        }

        return intent;
    }

    /**
     * A variant of {@link #getCallIntent} for starting a video call.
     */
    public static Intent getVideoCallIntent(String number, String callOrigin) {
        final Intent intent = new Intent(Intent.ACTION_CALL, getCallUri(number));
        intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                VideoProfile.STATE_BIDIRECTIONAL);
        if (!TextUtils.isEmpty(callOrigin)) {
            intent.putExtra(PhoneConstants.EXTRA_CALL_ORIGIN, callOrigin);
        }
        return intent;
    }

    /**
     * Return Uri with an appropriate scheme, accepting both SIP and usual phone call
     * numbers.
     */
    public static Uri getCallUri(String number) {
        if (PhoneNumberHelper.isUriNumber(number)) {
             return Uri.fromParts(PhoneAccount.SCHEME_SIP, number, null);
        }
        return Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
    }

    /**
     * Determines if one of the call capable phone accounts defined supports video calling.
     *
     * @param context The context.
     * @return {@code true} if one of the call capable phone accounts supports video calling,
     *      {@code false} otherwise.
     */
    private static boolean hasVideoCapability(Context context) {
        TelecomManager telecommMgr = (TelecomManager)
                context.getSystemService(Context.TELECOM_SERVICE);
        if (telecommMgr == null) {
            return false;
        }

        List<PhoneAccountHandle> accountHandles = telecommMgr.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle accountHandle : accountHandles) {
            PhoneAccount account = telecommMgr.getPhoneAccount(accountHandle);
            if (account != null && account.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the number is valid for videoCall
     *
     * @param number the number to call.
     * @return true if the number is valid
     *
     * @hide
     */
    public static boolean isVideoCallNumValid(String number){
        if (null == number) {
            return false;
        }
        if (number.contains("#") || number.contains("+") ||
                number.contains(",") || number.contains(";") ||
                number.contains("*")) {
            return false;
        }
        String norNumber = PhoneNumberHelper.normalizeNumber(number);
        if (norNumber.length() < MAX_PHONE_NUM) {
            return false;
        }
        return true;
    }

    public static boolean isVideoEnabled(Context context) {

        final int enableVideoCall = getVideoCallingConfig(context);

        if (enableVideoCall == ENABLE_VIDEO_CALLING) {
            Settings.System.putInt(context.getContentResolver(),
                    CONFIG_VIDEO_CALLING,ENABLE_VIDEO_CALLING);
            return true;
        } else if(enableVideoCall == DISABLE_VIDEO_CALLING) {
            Settings.System.putInt(context.getContentResolver(),
                    CONFIG_VIDEO_CALLING,DISABLE_VIDEO_CALLING);
            return false;
        } else {
            boolean hasVideoCap = hasVideoCapability(context);
            Settings.System.putInt(context.getContentResolver(),
                    CONFIG_VIDEO_CALLING,hasVideoCap?ENABLE_VIDEO_CALLING:DISABLE_VIDEO_CALLING);
            return hasVideoCap;
        }
    }

    private static int getVideoCallingConfig(Context context) {
        return context.getResources().getInteger(
                R.integer.config_enable_video_calling);
    }

    /**
     * Determines if one of the call capable phone accounts defined supports calling with a subject
     * specified.
     *
     * @param context The context.
     * @return {@code true} if one of the call capable phone accounts supports calling with a
     *      subject specified, {@code false} otherwise.
     */
    public static boolean isCallWithSubjectSupported(Context context) {
        TelecomManager telecommMgr = (TelecomManager)
                context.getSystemService(Context.TELECOM_SERVICE);
        if (telecommMgr == null) {
            return false;
        }

        List<PhoneAccountHandle> accountHandles = telecommMgr.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle accountHandle : accountHandles) {
            PhoneAccount account = telecommMgr.getPhoneAccount(accountHandle);
            if (account != null && account.hasCapabilities(PhoneAccount.CAPABILITY_CALL_SUBJECT)) {
                return true;
            }
        }
        return false;
    }
    /**
     * if true, conference dialer  is enabled.
     */
    public static boolean isConferDialerEnabled(Context context) {
        if (SystemProperties.getBoolean("persist.radio.conferdialer", false)) {
            TelephonyManager telephonyMgr = (TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            return telephonyMgr.isImsRegistered();
        }
        return false;
    }

    /**
     * get intent to start conference dialer
     * with this intent, we can originate an conference call
     */
    public static Intent getConferenceDialerIntent(String number) {
        Intent intent = new Intent("android.intent.action.ADDPARTICIPANT");
        intent.putExtra("confernece_number_key", number);
        return intent;
    }

    /**
     * used to get intent to start conference dialer
     * with this intent, we can add participants to an existing conference call
     */
    public static Intent getAddParticipantsIntent(String number) {
        Intent intent = new Intent("android.intent.action.ADDPARTICIPANT");
        intent.putExtra("add_participant", true);
        intent.putExtra("current_participant_list", number);
        return intent;
    }

    public static void createVideoCallingDialog(boolean isChecked ,final Context context) {
        int value = Settings.System.getInt(context.getContentResolver(),
                DIALOG_VIDEO_CALLING,DISABLE_VIDEO_CALLING);
        if(mAlertDialog == null && value == DISABLE_VIDEO_CALLING){
            View linearLayout = LayoutInflater.from(context).inflate(
                    R.layout.hint_dialog_layout, null);
            final CheckBox chkBox = (CheckBox) linearLayout
                    .findViewById(R.id.videocall);
            final Button btn = (Button) linearLayout
                    .findViewById(R.id.btn_ok);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mAlertDialog != null){
                        mAlertDialog.dismiss();
                        mAlertDialog = null;
                    }
                }
            });

            final TextView txtMessage = (TextView) linearLayout
                    .findViewById(R.id.txt_message);
            txtMessage.setText(
                isChecked?R.string.video_call_message_on : R.string.video_call_message_off);

            chkBox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                        boolean isChecked) {
                    Settings.System.putInt(context.getContentResolver(),
                        DIALOG_VIDEO_CALLING,isChecked?ENABLE_VIDEO_CALLING:DISABLE_VIDEO_CALLING);
                }
            });
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(linearLayout);
            builder.create().setCancelable(false);
            mAlertDialog = builder.show();
        }
    }

}
