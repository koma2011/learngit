LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

appcompat := appcompat
cardview := cardview
res_dirs := res $(appcompat)/res $(cardview)/res

LOCAL_STATIC_JAVA_LIBRARIES := support \
		               appcompat \
			       cardview

LOCAL_SRC_FILES := $(call all-java-files-under, acra) \
		   $(call all-java-files-under, support-v7-appcompat) \
		   $(call all-java-files-under, cardview) \
		   $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
LOCAL_ASSET_DIR := $(LOCAL_PATH)/assets
LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS := --auto-add-overlay \
    		    --extra-packages android.support.v7.appcompat \
    		    --extra-packages android.support.v7.cardview

#LOCAL_JNI_SHARED_LIBRARIES :=libgif

LOCAL_PACKAGE_NAME := MaterialGesture
#LOCAL_SDK_VERSION := current
LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_FLAG_FILES := proguard-project.txt                                                                                                                 
LOCAL_MODULE_TAGS := optional

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := support:appcompat/libs/android-support-v4.jar \
    					appcompat:appcompat/libs/android-support-v7-appcompat.jar \
					cardview:cardview/libs/android-support-v7-cardview.jar

include $(BUILD_MULTI_PREBUILT)
