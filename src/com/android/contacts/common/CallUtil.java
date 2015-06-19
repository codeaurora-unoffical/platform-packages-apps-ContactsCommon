/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.PhoneNumberUtils;

import com.android.contacts.common.util.PhoneNumberHelper;
import com.android.phone.common.PhoneConstants;

import java.util.List;

/**
 * Utilities related to calls.
 */
public class CallUtil {

    /*Enable Video calling irrespective of video capabilities*/
    private static final int ENABLE_VIDEO_CALLING = 1;
    /*Disable Video calling irrespective of video capabilities*/
    private static final int DISABLE_VIDEO_CALLING = 2;

    /**
     * Return an Intent for making a phone call. Scheme (e.g. tel, sip) will be determined
     * automatically.
     */
    public static Intent getCallIntent(String number) {
        return getCallIntent(number, null, null);
    }

    /**
     * Return an Intent for making a phone call. A given Uri will be used as is (without any
     * sanity check).
     */
    public static Intent getCallIntent(Uri uri) {
        return getCallIntent(uri, null, null);
    }

    /**
     * A variant of {@link #getCallIntent(String)} but also accept a call origin.
     * For more information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(String number, String callOrigin) {
        return getCallIntent(getCallUri(number), callOrigin, null);
    }

    /**
     * A variant of {@link #getCallIntent(String)} but also include {@code Account}.
     */
    public static Intent getCallIntent(String number, PhoneAccountHandle accountHandle) {
        return getCallIntent(number, null, accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also include {@code Account}.
     */
    public static Intent getCallIntent(Uri uri, PhoneAccountHandle accountHandle) {
        return getCallIntent(uri, null, accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(String, String)} but also include {@code Account}.
     */
    public static Intent getCallIntent(
            String number, String callOrigin, PhoneAccountHandle accountHandle) {
        return getCallIntent(getCallUri(number), callOrigin, accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also accept a call
     * origin and {@code Account}.
     * For more information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(
            Uri uri, String callOrigin, PhoneAccountHandle accountHandle) {
        return getCallIntent(uri, callOrigin, accountHandle,
                VideoProfile.VideoState.AUDIO_ONLY);
    }

    /**
     * get intent to start csvt.
     */
    public static Intent getCSVTCallIntent(String number) {
        Intent intent = new Intent("com.borqs.videocall.action.LaunchVideoCallScreen");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra("IsCallOrAnswer", true);
        intent.putExtra("LaunchMode", 1);
        intent.putExtra("call_number_key", number);
        return intent;
    }

    /**
     * if true, csvt is enabled.
     */
    public static boolean isCSVTEnabled() {
        boolean CSVTSupported = SystemProperties.getBoolean("persist.radio.csvt.enabled", false);
        return CSVTSupported;
    }

    /**
     * A variant of {@link #getCallIntent(String, String)} for starting a video call.
     */
    public static Intent getVideoCallIntent(String number, String callOrigin) {
        return getCallIntent(getCallUri(number), callOrigin, null,
                VideoProfile.VideoState.BIDIRECTIONAL);
    }

    /**
     * A variant of {@link #getCallIntent(String, String, android.telecom.PhoneAccountHandle)} for
     * starting a video call.
     */
    public static Intent getVideoCallIntent(
            String number, String callOrigin, PhoneAccountHandle accountHandle) {
        return getCallIntent(getCallUri(number), callOrigin, accountHandle,
                VideoProfile.VideoState.BIDIRECTIONAL);
    }

    /**
     * A variant of {@link #getCallIntent(String, String, android.telecom.PhoneAccountHandle)} for
     * starting a video call.
     */
    public static Intent getVideoCallIntent(String number, PhoneAccountHandle accountHandle) {
        return getVideoCallIntent(number, null, accountHandle);
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} for calling Voicemail.
     */
    public static Intent getVoicemailIntent() {
        return getCallIntent(Uri.fromParts(PhoneAccount.SCHEME_VOICEMAIL, "", null));
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also accept a call
     * origin and {@code Account} and {@code VideoCallProfile} state.
     * For more information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(
            Uri uri, String callOrigin, PhoneAccountHandle accountHandle, int videoState) {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, uri);
        intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, videoState);
        if (callOrigin != null) {
            intent.putExtra(PhoneConstants.EXTRA_CALL_ORIGIN, callOrigin);
        }
        if (accountHandle != null) {
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);
        }

        return intent;
    }

    /**
     * Return Uri with an appropriate scheme, accepting both SIP and usual phone call
     * Checks whether two phone numbers resolve to the same phone.
     */
    public static boolean phoneNumbersEqual(String number1, String number2) {
        if (PhoneNumberUtils.isUriNumber(number1) || PhoneNumberUtils.isUriNumber(number2)) {
            return sipAddressesEqual(number1, number2);
        } else {
            return PhoneNumberUtils.compare(number1, number2);
        }
    }

    private static boolean sipAddressesEqual(String number1, String number2) {
        if (number1 == null || number2 == null) return number1 == number2;

        int index1 = number1.indexOf('@');
        final String userinfo1;
        final String rest1;
        if (index1 != -1) {
            userinfo1 = number1.substring(0, index1);
            rest1 = number1.substring(index1);
        } else {
            userinfo1 = number1;
            rest1 = "";
        }

        int index2 = number2.indexOf('@');
        final String userinfo2;
        final String rest2;
        if (index2 != -1) {
            userinfo2 = number2.substring(0, index2);
            rest2 = number2.substring(index2);
        } else {
            userinfo2 = number2;
            rest2 = "";
        }

        return userinfo1.equals(userinfo2) && rest1.equalsIgnoreCase(rest2);
    }

    /**
     * Return Uri with an appropriate scheme, accepting Voicemail, SIP, and usual phone call
     * numbers.
     */
    public static Uri getCallUri(String number) {
        if (PhoneNumberHelper.isUriNumber(number)) {
             return Uri.fromParts(PhoneAccount.SCHEME_SIP, number, null);
        }
        return Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
     }

    private static boolean hasCapability(PhoneAccount phoneAccount, int capability) {
        return (phoneAccount != null) &&
                ((phoneAccount.getCapabilities() & capability) == capability);
    }

    private static boolean hasVideoCapability(Context context) {
        TelecomManager telecommMgr = (TelecomManager)
                context.getSystemService(Context.TELECOM_SERVICE);
        if (telecommMgr == null) {
            return false;
        }
        List<PhoneAccountHandle> phoneAccountHandles =
                telecommMgr.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle handle : phoneAccountHandles) {
            final PhoneAccount phoneAccount = telecommMgr.getPhoneAccount(handle);
            if (hasCapability(phoneAccount, PhoneAccount.CAPABILITY_VIDEO_CALLING)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVideoEnabled(Context context) {

        int enableVideoCall = getVideoCallingConfig(context);

        if (enableVideoCall == ENABLE_VIDEO_CALLING) {
            return true;
        } else if(enableVideoCall == DISABLE_VIDEO_CALLING) {
            return false;
        } else {
            return hasVideoCapability(context);
        }
    }

    private static int getVideoCallingConfig(Context context) {
        return context.getResources().getInteger(
                R.integer.config_enable_video_calling);
    }

    /**
     * if true, conference dialer  is enabled.
     */
    public static boolean isConferDialerEnabled() {
        return SystemProperties.getBoolean("persist.radio.conferdialer", false);
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

    /**
     *  This method is used for check the phone number is valid for make video call.
     *  if return true, number pattern check is passed and video call is allowed.
     *  Otherwise the phone number didn't allow make video call.
     * @param number
     * @return weather the number formatter match the carriers requirement
     */
    public static boolean isVideoCallNumValid(Context context, String number){
        if(!context.getResources().getBoolean(
                com.android.internal.R.bool.config_regional_number_patterns_video_call)){
            return true;
        }
        String norNumber = PhoneNumberHelper.normalizeNumber(number);
        if (norNumber == null || "".equals(norNumber) ||
                ((norNumber.startsWith("+") ? norNumber.length() > 8 : norNumber.length() > 7))
                || number.contains("#") || number.contains("*")) {
            return false;
        } else {
            return true;
        }
    }
}
