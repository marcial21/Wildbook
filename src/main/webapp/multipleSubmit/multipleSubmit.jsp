<%@ page contentType="text/html; charset=utf-8" language="java" 
        import="org.ecocean.CommonConfiguration,
        java.util.Properties, 
        org.ecocean.servlet.ServletUtilities,
        org.ecocean.*,
        java.util.Properties,
        java.util.List,
        java.util.ArrayList"        
%>

<%
/*
Hello! This page consists mostly of anchor points that we add components to using JS. 
The JS you want is in /javascript/multipleSubmit/
*/

    String langCode = ServletUtilities.getLanguageCode(request);
    String context=ServletUtilities.getContext(request);
    Properties props = new Properties();
    props = ShepherdProperties.getProperties("multipleSubmit.properties", langCode,context);
    Properties recaptchaProps = new Properties();
    recaptchaProps = ShepherdProperties.getProperties("recaptcha.properties", "");
    long maxMediaSize = CommonConfiguration.getMaxMediaSizeInMegabytes(context);
%>

<script> 
// Only use to convey property values to JS file
var tempBytes = "<%=maxMediaSize%>";
console.log("tempBytes (in MB) = "+tempBytes);
if (tempBytes!=""&&tempBytes!=undefined&&!isNaN(tempBytes)) {
    maxBytes = (parseInt(tempBytes)*1048576);
}
</script>

<jsp:include page="../header.jsp" flush="true"/>
<div id="root-div" class="container-fluid maincontent">

    <div class="row">
        <div class="col-xs-12 col-lg-12">
            <div class="container">
                <h2><%= props.getProperty("pageHeader")%></h2>
                <p><b><%= props.getProperty("headerDesc")%></b></p>
                <p>[ We urge you to follow this link to the instructions if you have not used this feature before. ]</p>
            </div>
            <hr>

            <form id="multipleSubmission">

                <!-- specify number of encounters in two input items -->

                <div class="container form-file-selection">
                    <label><%= props.getProperty("specifyEncNum")%></label>
                    <input id="number-encounters" type="number" name="number-encounters" required value="1" min="1" max="48">
                    <input class="btn btn-large btn-file-selector" type="button" onclick="document.getElementById('file-selector-input').click()" value="Select Files" />
                </div>
                <input id="file-selector-input" name="allFiles" class="hidden-input" type="file" accept=".jpg, .jpeg, .png, .bmp, .gif, .mov, .wmv, .avi, .mp4, .mpg" style="display:none;" multiple size="50" onChange="updateSelected(this);" />
                <div><p id="input-file-list"></p></div> 
                <br>

                <!-- easy place to store this -->
                <input id="recaptcha-checked" name="recaptcha-checked" type="hidden" value="false" />


                <div class="recaptcha-div hidden-input form-define-metadata">

                    <!-- Recaptcha widget -->
                    <div id="recaptcha-div">
                        <%= ServletUtilities.captchaWidget(request) %>
                    </div>

                </div>

                <br>

                <!-- Here is where we are going to put UI to define encounter metadata from JS -->
                <div id="metadata-tiles-main" class="row">
                
                </div>

                <br>

                <!-- Here is where we are going to dump rendered images and encounter UI from JS -->
                <div id="image-tiles-main" class="row">
                
                </div>

                <div class="container">

                    <hr>
                    <!-- next page -->
                    <button class="" id="continueButton" type="button" disabled onclick="continueButtonClicked();"><%= props.getProperty("continue")%></button>

                    <!-- back to file selection -->
                    <button class="hidden-input" id="backButton" type="button" onclick="backButtonClicked();"><%= props.getProperty("back")%></button>

                    <!-- actually done now, send it off -->
                    <button class="hidden-input" id="sendButton" type="button" disabled onclick="sendButtonClicked();"><%= props.getProperty("complete")%></button>

                </div>

                <!-- display text for java exceptions recieved from server -->
                <div id="server-error"></div>
            </form>
        </div> 
        <hr>
    </div>
</div>

<jsp:include page="../footer.jsp" flush="true"/>
