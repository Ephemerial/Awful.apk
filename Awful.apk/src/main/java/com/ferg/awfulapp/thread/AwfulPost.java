/********************************************************************************
 * Copyright (c) 2011, Scott Ferguson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the software nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY SCOTT FERGUSON ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SCOTT FERGUSON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.ferg.awfulapp.thread;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.preferences.AwfulPreferences;
import com.ferg.awfulapp.provider.DatabaseHelper;
import com.ferg.awfulapp.provider.DatabaseHelper;
import com.ferg.awfulapp.util.AwfulUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwfulPost {
    private static final String TAG = "AwfulPost";

    public static final String PATH     = "/post";
    public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY + PATH);

    private static final Pattern fixCharacters_regex = Pattern.compile("([\\r\\f])");
	private static final Pattern youtubeId_regex = Pattern.compile("/v/([\\w_-]+)&?");
	private static final Pattern youtubeHDId_regex = Pattern.compile("/embed/([\\w_-]+)&?");
	private static final Pattern vimeoId_regex = Pattern.compile("clip_id=(\\d+)&?");
    private static final Pattern userid_regex = Pattern.compile("userid=(\\d+)");

    private static final List<String> HTTPS_SUPPORTED_DOMAINS =
            Collections.unmodifiableList(Arrays.asList("imgur.com", "somethingawful.com", "giphy.com"));

    public static final String ID                    = "_id";
    public static final String POST_INDEX            = "post_index";
    public static final String THREAD_ID             = "thread_id";
    public static final String DATE                  = "date";
	public static final String REGDATE				 = "regdate";
    public static final String USER_ID               = "user_id";
    public static final String USERNAME              = "username";
    public static final String PREVIOUSLY_READ       = "previously_read";
    public static final String EDITABLE              = "editable";
    public static final String IS_OP                 = "is_op";
    public static final String IS_ADMIN              = "is_admin";
    public static final String IS_MOD                = "is_mod";
    public static final String IS_PLAT               = "is_plat";
    public static final String AVATAR                = "avatar";
	public static final String AVATAR_TEXT 			 = "avatar_text";
    public static final String CONTENT               = "content";
    public static final String EDITED                = "edited";

	public static final String FORM_KEY = "form_key";
	public static final String FORM_COOKIE = "form_cookie";
    public static final String FORM_BOOKMARK = "bookmark";
    public static final String FORM_SIGNATURE = "signature";
    public static final String FORM_DISABLE_SMILIES = "disablesmilies";
	/** For comparing against replies to see if the user actually typed anything. **/
	public static final String REPLY_ORIGINAL_CONTENT = "original_reply";
	public static final String EDIT_POST_ID = "edit_id";




	private int mThreadId = -1;
    private String mId = "";
    private String mDate = "";
    private String mRegDate = "";
    private String mUserId = "";
    private String mUsername = "";
    private String mAvatar = "";
    private String mAvatarText = "";
    private String mContent = "";
    private String mEdited = "";

	private boolean mPreviouslyRead = false;
    private String mLastReadUrl = "";
    private boolean mEditable;
    private boolean isOp = false;
    private boolean isAdmin = false;
    private boolean isMod = false;
    private boolean isPlat = false;


    public JSONObject toJSON() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("id", mId);
        result.put("date", mDate);
        result.put("user_id", mUserId);
        result.put("username", mUsername);
        result.put("avatar", mAvatar);
        result.put("content", mContent);
        result.put("edited", mEdited);
        result.put("previouslyRead", Boolean.toString(mPreviouslyRead));
        result.put("lastReadUrl", mLastReadUrl);
        result.put("editable", Boolean.toString(mEditable));
        result.put("isOp", Boolean.toString(isOp()));
        result.put("isPlat", Boolean.toString(isPlat()));

        return result;
    }

    public boolean isOp() {
    	return isOp;
    }

    public boolean isAdmin() {
    	return isAdmin;
    }

    public boolean isMod() {
    	return isMod;
    }

    public boolean isPlat() {
        return isPlat;
    }

    public String getId() {
        return mId;
    }

    public void setId(String aId) {
        mId = aId;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String aDate) {
        mDate = aDate;
    }

	public String getRegDate() {
		return mRegDate;
	}

    public void setRegDate(String aRegDate) {
        mRegDate = aRegDate;
    }

    public String getUserId() {
    	return mUserId;
    }

    public void setUserId(String aUserId) {
    	mUserId = aUserId;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String aUsername) {
        mUsername = aUsername;
    }

    public void setThreadId(int aThreadId) {
        mThreadId = aThreadId;
    }

    public int getThreadId() {
        return mThreadId;
    }

    public void setIsOp(boolean aIsOp) {
        isOp = aIsOp;
    }

    public void setIsAdmin(boolean aIsAdmin) {
        isAdmin = aIsAdmin;
    }

    public void setIsMod(boolean aIsMod) {
        isMod = aIsMod;
    }

    public void setIsPlat(boolean plat) {
        isPlat = plat;
    }

    public String getAvatar() {
        return mAvatar;
    }

    public void setAvatar(String aAvatar) {
        mAvatar = aAvatar;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String aContent) {
        mContent = aContent;
    }

    public static ArrayList<AwfulPost> fromCursor(Context aContext, Cursor aCursor) {
        ArrayList<AwfulPost> result = new ArrayList<AwfulPost>();

        if (aCursor.moveToFirst()) {
            int idIndex = aCursor.getColumnIndex(ID);
            int threadIdIndex = aCursor.getColumnIndex(THREAD_ID);
            int postIndexIndex = aCursor.getColumnIndex(POST_INDEX);//ooh, meta
            int dateIndex = aCursor.getColumnIndex(DATE);
            int regdateIndex = aCursor.getColumnIndex(REGDATE);
            int userIdIndex = aCursor.getColumnIndex(USER_ID);
            int usernameIndex = aCursor.getColumnIndex(USERNAME);
            int previouslyReadIndex = aCursor.getColumnIndex(PREVIOUSLY_READ);
            int editableIndex = aCursor.getColumnIndex(EDITABLE);
            int isOpIndex = aCursor.getColumnIndex(IS_OP);
            int isAdminIndex = aCursor.getColumnIndex(IS_ADMIN);
            int isModIndex = aCursor.getColumnIndex(IS_MOD);
            int isPlatIndex = aCursor.getColumnIndex(IS_PLAT);
            int avatarIndex = aCursor.getColumnIndex(AVATAR);
            int avatarTextIndex = aCursor.getColumnIndex(AVATAR_TEXT);
            int contentIndex = aCursor.getColumnIndex(CONTENT);
            int editedIndex = aCursor.getColumnIndex(EDITED);

            AwfulPost current;

            do {
                current = new AwfulPost();
                current.setId(aCursor.getString(idIndex));
                current.setThreadId(aCursor.getInt(threadIdIndex));
                current.setDate(aCursor.getString(dateIndex));
                current.setRegDate(aCursor.getString(regdateIndex));
                current.setUserId(aCursor.getString(userIdIndex));
                current.setUsername(aCursor.getString(usernameIndex));
                current.setPreviouslyRead(aCursor.getInt(previouslyReadIndex) > 0);
                current.setLastReadUrl(aCursor.getInt(postIndexIndex)+"");
                current.setEditable(aCursor.getInt(editableIndex) == 1);
                current.setIsOp(aCursor.getInt(isOpIndex) == 1);
                current.setIsAdmin(aCursor.getInt(isAdminIndex) > 0);
                current.setIsMod(aCursor.getInt(isModIndex) > 0);
                current.setIsPlat(aCursor.getInt(isPlatIndex) > 0);
                current.setAvatar(aCursor.getString(avatarIndex));
                current.setAvatarText(aCursor.getString(avatarTextIndex));
                current.setContent(aCursor.getString(contentIndex));
                current.setEdited(aCursor.getString(editedIndex));

                result.add(current);
            } while (aCursor.moveToNext());
        }else{
        	Log.i(TAG,"No posts to convert.");
        }
        return result;
    }


    /**
     * Process any videos found within an Element's hierarchy.
     *
     * This will look for appropriate video elements, and rewrite or replace them as necessary so
     * the app can display them according to the user's preferences.
     * This mutates the supplied Element's structure.
     * @param contentNode       the Element to search and edit
     * @param inlineYouTubes    whether YouTube videos should be displayed inline, or replaced with a link
     */
    public static void convertVideos(Element contentNode, boolean inlineYouTubes){

			Elements youtubeNodes = contentNode.getElementsByClass("youtube-player");

        for (Element youTube : youtubeNodes) {
            try {
                String src = youTube.attr("src");
                //int height = Integer.parseInt(youTube.attr("height"));
                //int width = Integer.parseInt(youTube.attr("width"));
                Matcher youtubeMatcher = youtubeHDId_regex.matcher(src);
                if (youtubeMatcher.find()) {
                    String videoId = youtubeMatcher.group(1);
                    String link = "http://www.youtube.com/watch?v=" + videoId;
                    String image = "http://img.youtube.com/vi/" + videoId + "/0.jpg";

                    if (AwfulUtils.isKitKatOnly() && !Build.VERSION.RELEASE.equals("4.4.4")) {
                        Element youtubeContainer = new Element(Tag.valueOf("div"), "");
                        youtubeContainer.attr("style", "position: relative;text-align: center;background-color: transparent;");
                        Element youtubeLink = new Element(Tag.valueOf("a"), "");
                        youtubeLink.attr("href", link);
                        youtubeLink.attr("style", "position: absolute; background:url('file:///android_res/drawable/ic_menu_video.png') no-repeat center center;    top: 0; right: 0; bottom: 0; left: 0;");
                        Element img = new Element(Tag.valueOf("img"), "");
                        img.attr("class", "nolink videoPlayButton");
                        img.attr("src", image);
                        img.attr("style", "max-width: 100%;");
                        youtubeContainer.appendChild(img);
                        youtubeContainer.appendChild(youtubeLink);
                        youTube.replaceWith(youtubeContainer);
                    } else if (!inlineYouTubes) {
                        Element youtubeLink = new Element(Tag.valueOf("a"), "");
                        youtubeLink.text(link);
                        youtubeLink.attr("href", link);
                        youTube.replaceWith(youtubeLink);
                    } else {
                        Element youtubeLink = new Element(Tag.valueOf("a"), "");
                        youtubeLink.text(link);
                        youtubeLink.attr("href", link);
                        youTube.after(youtubeLink);
                        youtubeLink.before(new Element(Tag.valueOf("br"), ""));

                        Element youtubeContainer = new Element(Tag.valueOf("div"), "");
                        youtubeContainer.addClass("videoWrapper");
                        youTube.before(youtubeContainer);
                        youtubeContainer.appendChild(youTube);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed youtube convertion:", e);
                continue; //if we fail to convert the video tag, we can still display the rest.
            }
        }

        Elements videoNodes = contentNode.getElementsByClass("bbcode_video");
        for (Element node : videoNodes) {
            try {
                String src = null;
                int height = 0;
                int width = 0;
                Elements object = node.getElementsByTag("object");
                if (object.size() > 0) {
                    height = Integer.parseInt(object.get(0).attr("height"));
                    width = Integer.parseInt(object.get(0).attr("width"));
                    Elements emb = object.get(0).getElementsByTag("embed");
                    if (emb.size() > 0) {
                        src = emb.get(0).attr("src");
                    }
                }
                if (src != null && height != 0 && width != 0) {
                    String link = null, image = null;
                    Matcher youtube = youtubeId_regex.matcher(src);
                    Matcher vimeo = vimeoId_regex.matcher(src);
                    if (vimeo.find()) {
                        String videoId = vimeo.group(1);
                        Element vimeoXML;
                        try {
                            vimeoXML = NetworkUtils.get("http://vimeo.com/api/v2/video/" + videoId + ".xml");
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                        if (vimeoXML.getElementsByTag("mobile_url").first() != null) {
                            link = vimeoXML.getElementsByTag("mobile_url").first().text();
                        } else {
                            link = vimeoXML.getElementsByTag("url").first().text();
                        }
                        image = vimeoXML.getElementsByTag("thumbnail_large").first().text();
                        src = link;
                    } else {
                        node.empty();
                        Element ln = new Element(Tag.valueOf("a"), "");
                        ln.attr("href", src);
                        ln.text(src);
                        node.replaceWith(ln);
                        continue;
                    }

                    if (inlineYouTubes && (AwfulUtils.isKitKatOnly() && !Build.VERSION.RELEASE.equals("4.4.4"))) {
                        Element nodeContainer = new Element(Tag.valueOf("div"), "");
                        nodeContainer.attr("style", "position: relative;text-align: center;background-color: transparent;");
                        Element nodeLink = new Element(Tag.valueOf("a"), "");
                        nodeLink.attr("href", link);
                        nodeLink.attr("style", "position: absolute; background:url('file:///android_res/drawable/ic_menu_video.png') no-repeat center center;    top: 0; right: 0; bottom: 0; left: 0;");
                        Element img = new Element(Tag.valueOf("img"), "");
                        img.attr("class", "nolink videoPlayButton");
                        img.attr("src", image);
                        img.attr("style", "max-width: 100%;");
                        nodeContainer.appendChild(img);
                        nodeContainer.appendChild(nodeLink);
                        node.replaceWith(nodeContainer);
                    } else {
                        node.empty();
                        Element ln = new Element(Tag.valueOf("a"), "");
                        ln.attr("href", link);
                        ln.text(link);
                        node.replaceWith(ln);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed video convertion:", e);
                continue;//if we fail to convert the video tag, we can still display the rest.
            }
        }
    }

	public String getEdited() {
        return mEdited;
    }

    public void setEdited(String aEdited) {
        mEdited = aEdited;
    }

    public String getAvatarText() {
    	return mAvatarText;
	}

    public void setAvatarText(String text) {
    	mAvatarText = text;
	}


    public String getLastReadUrl() {
        return mLastReadUrl;
    }

    public void setLastReadUrl(String aLastReadUrl) {
        mLastReadUrl = aLastReadUrl;
    }

	public boolean isPreviouslyRead() {
		return mPreviouslyRead;
	}

	public void setPreviouslyRead(boolean aPreviouslyRead) {
		mPreviouslyRead = aPreviouslyRead;
	}

	public boolean isEditable() {
		return mEditable;
	}

	public void setEditable(boolean aEditable) {
		mEditable = aEditable;
	}


    public static void syncPosts(ContentResolver content, Document aThread, int aThreadId, int unreadIndex, int opId, AwfulPreferences prefs, int startIndex){
        ArrayList<ContentValues> result = AwfulPost.parsePosts(aThread, aThreadId, unreadIndex, opId, prefs, startIndex, false);

        int resultCount = content.bulkInsert(CONTENT_URI, result.toArray(new ContentValues[result.size()]));
        Log.i(TAG, "Inserted "+resultCount+" posts into DB, threadId:"+aThreadId+" unreadIndex: "+unreadIndex);
    }

    public static ArrayList<ContentValues> parsePosts(Document aThread, int aThreadId, int unreadIndex, int opId, AwfulPreferences prefs, int startIndex, boolean preview){
    	ArrayList<ContentValues> result = new ArrayList<>();
    	boolean foundUnreadPost = false;
		int index = startIndex;
        String update_time = new Timestamp(System.currentTimeMillis()).toString();

        Elements posts = aThread.getElementsByClass(preview ? "standard" : "post");
        for(Element postData : posts){
            if (postData.hasClass("ignored") && prefs.hideIgnoredPosts) {
                continue;
            }

            ContentValues post = new ContentValues();
            //timestamp for DB trimming after a week
            post.put(DatabaseHelper.UPDATED_TIMESTAMP, update_time);
        	post.put(THREAD_ID, aThreadId);

        	if(!preview) {
                //post id is formatted "post1234567", so we strip out the "post" prefix.
                post.put(ID, Integer.parseInt(postData.id().replaceAll("\\D", "")));
                //we calculate this beforehand, but now can pull this from the post (thanks cooch!)
                //wait actually no, FYAD doesn't support this. ~FYAD Privilege~
                try {
                    post.put(POST_INDEX, Integer.parseInt(postData.attr("data-idx").replaceAll("\\D", "")));
                } catch (NumberFormatException nfe) {
                    post.put(POST_INDEX, index);
                }
            }

            // Check for "class=seenX", or just rely on unread index
            boolean markedSeen = false;
            for (String aClass : postData.classNames()) {
                if (aClass.toLowerCase().startsWith("seen")) {
                    markedSeen = true;
                    break;
                }
            }
            if(!markedSeen && index > unreadIndex){
            	post.put(PREVIOUSLY_READ, 0);
            	foundUnreadPost = true;
            }else{
            	post.put(PREVIOUSLY_READ, 1);
            }
            index++;

            //set these to 0 now, update them if needed, probably should have used a default value in the SQL table
			post.put(IS_MOD, 0);
            post.put(IS_ADMIN, 0);
            post.put(IS_PLAT, 0);

            //rather than repeatedly query for specific classes, we are just going to grab them all and run through them all
            Elements postClasses = postData.getElementsByAttribute("class");
            for(Element entry: postClasses){
            	String type = entry.attr("class");

                if (type.contains("author")) {
                    post.put(USERNAME, entry.text());
                }
            	if (type.contains("registered")) {
					post.put(REGDATE, entry.text());
				}
                if (type.contains("platinum")) {
                    post.put(IS_PLAT, 1);
                }
				if (type.contains("role-mod")) {
					post.put(IS_MOD, 1);
				}
				if (type.contains("role-admin")) {
                    post.put(IS_ADMIN, 1);
				}

				if (type.equalsIgnoreCase("title") && entry.children().size() > 0) {
					Elements avatar = entry.getElementsByTag("img");
					if (avatar.size() > 0) {
                        tryConvertToHttps(avatar.get(0));
						post.put(AVATAR, avatar.get(0).attr("src"));
					}
					post.put(AVATAR_TEXT, entry.text());
				}

                if(type.equalsIgnoreCase("inner postbody")){
                    entry.removeClass("inner");
                    type = entry.attr("class");
                }

				if (type.equalsIgnoreCase("postbody") && !(entry.getElementsByClass("complete_shit").size() > 0) || type.contains("complete_shit")) {
                    convertVideos(entry, prefs.inlineYoutube);
                    for(Element img : entry.getElementsByTag("img")) {
                        processPostImage(img, !foundUnreadPost, prefs);
                    }
                    for (Element link : entry.getElementsByTag("a")) {
                        tryConvertToHttps(link);
                    }
                    post.put(CONTENT, entry.html());
                }

                if (type.equalsIgnoreCase("postdate")) {
					post.put(DATE, NetworkUtils.unencodeHtml(entry.text()).replaceAll("[^\\w\\s:,]", "").trim());
				}

				if (type.startsWith("userinfo userid-")) {
                    int userId = Integer.parseInt(type.substring(16));
                    post.put(USER_ID, userId);
                    post.put(IS_OP, (opId == userId ? 1 : 0));
                }
                if (type.equalsIgnoreCase("editedby") && entry.children().size() > 0) {
					post.put(EDITED, "<i>" + entry.children().get(0).text() + "</i>");
                }
                if (type.equalsIgnoreCase("profilelinks") && !post.containsKey(USER_ID)) {
                    Elements userlink = entry.getElementsByAttributeValueContaining("href", "userid=");

                    if (userlink.size() > 0) {
                        Matcher userid = userid_regex.matcher(userlink.get(0).attr("href"));
                        if(userid.find()){
                            int uid = Integer.parseInt(userid.group(1));
                            post.put(USER_ID, uid);
                            post.put(IS_OP, (opId == uid ? 1 : 0));
                        }else{
                            Log.e(TAG, "Failed to parse UID!");
                        }
                    }else{
                        Log.e(TAG, "Failed to parse UID!");
                    }
                }

            }
            post.put(EDITABLE, postData.getElementsByAttributeValue("alt", "Edit").size());
            result.add(post);
        }
        Log.i(TAG, Integer.toString(posts.size()) + " posts found, " + result.size() + " posts parsed.");
    	return result;
    }


    /**
     * Process an img element from a post, to make it display correctly in the app.
     * <p>
     * This performs any necessary conversion, referring to user preferences to determine if e.g.
     * images should be loaded or converted to a link. This mutates the supplied Element.
     *
     * @param img        an Element represented by an img tag
     * @param isOldImage whether this is an old image (from a previously seen post), may be hidden
     * @param prefs      preferences used to make decisions
     */
    public static void processPostImage(Element img, boolean isOldImage, AwfulPreferences prefs) {
        //don't alter video mock buttons
        if (img.hasClass("videoPlayButton")) {
            return;
        }
        tryConvertToHttps(img);
        boolean isTimg = img.hasClass("timg");
        String originalUrl = img.attr("src");

        // check whether images can be converted to / wrapped in a link
        boolean alreadyLinked = img.parent() != null && img.parent().tagName().equalsIgnoreCase("a");
        boolean linkOk = !alreadyLinked && !img.hasClass("nolink");

        // image is a smiley - if required, replace it with its :code: (held in the 'title' attr)
        if (img.hasAttr("title")) {
            if (!prefs.showSmilies) {
                String name = img.attr("title");
                img.replaceWith(new Element(Tag.valueOf("p"), "").text(name));
            }
            return;
        }

        // image shouldn't be displayed - convert to link / plaintext url
        if (isOldImage && prefs.hideOldImages || !prefs.canLoadImages()) {
            if (linkOk) {
                // switch out for a link with the url
                img.replaceWith(new Element(Tag.valueOf("a"), "").attr("href", originalUrl).text(originalUrl));
            } else {
                img.replaceWith(new Element(Tag.valueOf("p"), "").text(originalUrl));
            }
            return;
        }

        // normal image - if we can't link it (e.g. to turn into an expandable thumbnail) there's nothing else to do
        if (!linkOk) {
            return;
        }

        // handle linking, thumbnailing, gif conversion etc

        // default to the 'thumbnail' url just being the full image
        String thumbUrl = originalUrl;

        // thumbnail any imgur images according to user prefs, if set
        if (!prefs.imgurThumbnails.equals("d") && thumbUrl.contains("i.imgur.com")) {
            thumbUrl = imgurAsThumbnail(thumbUrl, prefs.imgurThumbnails);
        }

        // handle gifs - different cases for different sites
        if (prefs.disableGifs && StringUtils.containsIgnoreCase(thumbUrl, ".gif")) {
            if (StringUtils.containsIgnoreCase(thumbUrl, "imgur.com")) {
                thumbUrl = imgurAsThumbnail(thumbUrl, "h");
            } else if (StringUtils.containsIgnoreCase(thumbUrl, "i.kinja-img.com")) {
                thumbUrl = thumbUrl.replace(".gif", ".jpg");
            } else if (StringUtils.containsIgnoreCase(thumbUrl, "giphy.com")) {
                thumbUrl = thumbUrl.replace("://i.giphy.com", "://media.giphy.com/media");
                if (thumbUrl.endsWith("giphy.gif")) {
                    thumbUrl = thumbUrl.replace("giphy.gif", "200_s.gif");
                } else {
                    thumbUrl = thumbUrl.replace(".gif", "/200_s.gif");
                }
            } else if (StringUtils.containsIgnoreCase(thumbUrl, "giant.gfycat.com")) {
                thumbUrl = thumbUrl.replace("giant.gfycat.com", "thumbs.gfycat.com");
                thumbUrl = thumbUrl.replace(".gif", "-poster.jpg");
            } else {
                thumbUrl = "file:///android_res/drawable/gif.png";
                img.attr("width", "200px");
            }

            // link and rewrite image, setting the link as click-to-play
            thumbnailAndLink(img, thumbUrl).addClass("playGif");
            return;
        }

        // non-gif images - wrap them in a link, unless handling as a TIMG (to avoid breaking its click behaviour)
        if (!isTimg || prefs.disableTimgs) {
            // if the image hasn't been processed then thumbUrl will be the original image URL, i.e. a full-size image
            thumbnailAndLink(img, thumbUrl);
        }
    }


    /**
     * Rewrite a Imgur image url as a thumbnailed version, if possible (e.g. not already a thumbnail).
     *
     * @param imgurUrl      the image url to rewrite
     * @param thumbnailCode the type of thumbnail, usually a single character code
     * @return the rewritten url, or the original if it couldn't be rewritten
     */
    @NonNull
    private static String imgurAsThumbnail(@NonNull String imgurUrl, @NonNull String thumbnailCode) {
        int lastDot = imgurUrl.lastIndexOf('.');
        int lastSlash = imgurUrl.lastIndexOf('/');
        String imgurImageId = imgurUrl.substring(lastSlash + 1, lastDot);
        //check if already thumbnails
        if (imgurImageId.length() != 6 && imgurImageId.length() != 8) {
            imgurUrl = imgurUrl.substring(0, lastDot) + thumbnailCode + imgurUrl.substring(lastDot);
        }
        return imgurUrl;
    }


    /**
     * Rewrite an image element as a thumbnail, wrapping it in a link to the original image URL.
     * <p>
     * The image will be replaced in the DOM by the anchor element, with the image as its child, and
     * the anchor returned. Classes on the image element are cleared.
     *
     * @param img          the image element
     * @param thumbnailUrl the URL for the wrapped image
     * @return the link element, containing the modified image element
     */
    @NonNull
    private static Element thumbnailAndLink(@NonNull Element img, @NonNull String thumbnailUrl) {
        Element link = new Element(Tag.valueOf("a"), "").attr("href", img.attr("src"));
        // rewrite the image (new src, no classes) and wrap it with the link in the DOM
        img.attr("src", thumbnailUrl);
        img.classNames(Collections.emptySet());
        img.replaceWith(link);
        link.appendChild(img);
        return link;
    }


    /**
     * Converts URLs to https versions, where appropriate.
     * <p>
     * This mutates the element directly.
     */
    public static void tryConvertToHttps(@NonNull Element element) {
        String attr;
        String url;

        // get the element's url attribute, give up if it doesn't have one
        if (element.hasAttr("href")) {
            attr = "href";
        } else if (element.hasAttr("src")) {
            attr = "src";
        } else {
            return;
        }

        // if the element's url is for a https-able domain, rewrite it
        for (String domain : HTTPS_SUPPORTED_DOMAINS) {
            url = element.attr(attr);
            if (StringUtils.containsIgnoreCase(url, domain)) {
                element.attr(attr, url.replace("http://", "https://"));
                return;
            }
        }
    }

}
