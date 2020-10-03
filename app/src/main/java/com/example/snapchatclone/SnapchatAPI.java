package com.example.snapchatclone;

import android.os.Handler;
import android.util.Log;

import com.amplifyframework.api.aws.GsonVariablesSerializer;
import com.amplifyframework.api.graphql.GraphQLOperation;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.PaginatedResult;
import com.amplifyframework.api.graphql.SimpleGraphQLRequest;
import com.amplifyframework.api.graphql.model.ModelMutation;
import com.amplifyframework.api.graphql.model.ModelPagination;
import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.generated.model.DirectMessageChat;
import com.amplifyframework.datastore.generated.model.DirectMessageEditor;
import com.amplifyframework.datastore.generated.model.FriendRequest;
import com.amplifyframework.datastore.generated.model.FriendRequestResponse;
import com.amplifyframework.datastore.generated.model.Message;
import com.amplifyframework.datastore.generated.model.Snap;
import com.amplifyframework.datastore.generated.model.User;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class SnapchatAPI {

    private static GraphQLOperation<FriendRequest> mFriendRequestSubscription = null;
    private static GraphQLOperation<Message> mMessageSubscription = null;
    private static GraphQLOperation<FriendRequestResponse> mFriendRequestResponseSubscription;
    private static GraphQLOperation<Snap> mSnapSubscription = null;

    /*
     * Private constructor so it cannot be instantiated
     * */
    private SnapchatAPI() {

    }

    private static void handleServerResponse(Handler handler, int code, Object data) {
        handler.sendMessage(handler.obtainMessage(code, data));
    }

    private static void handleServerResponse(Handler handler, int code) {
        handler.sendEmptyMessage(code);
    }

    /**
     * Sign user out through Amplify.Auth
     */
    public static void SignOut() {//(Handler handler, int responseCode) {
        Amplify.Auth.signOut(
                () -> {
                    //Log.i("AuthQuickstart", "Signed out successfully");
                },
                error -> Log.e("AuthQuickstart", error.toString())
        );
    }

    /**
     * Use amplify api to find users
     * return results to msg handler running on main thread to modify ui
     *
     * @param username username to search for
     */
    public static void QueryUsersByUsernameContains(Handler handler, int responseCode, String neUserId, String username, int page_limit, GraphQLRequest<PaginatedResult<User>> nextPageQuery) {

        GraphQLRequest<PaginatedResult<User>> request = null;
        //ListUser
        if (nextPageQuery == null) {
            request = ModelQuery.list(
                    User.class, User.USER_ID.ne(neUserId)//not the currently signed in user
                            .and(
                                    User.USERNAME.contains(username)),
                    ModelPagination.limit(page_limit));

        } else {
            request = nextPageQuery;
        }
        Amplify.API.query(
                //request users that have username in their username
                request,
                response -> {
                    if (response.hasData()) {
                        //Log.i("AmplifyQuery","User search results!");
                        //for each item in the page add to the recycler view
                        //send back to ui thread so we can update the user item view
                        handleServerResponse(handler, responseCode, response.getData());
                        if (response.getData().hasNextResult()) {
                            QueryUsersByUsernameContains(
                                    handler,
                                    responseCode,
                                    neUserId,
                                    username,
                                    page_limit, response.getData().getRequestForNextResult());
                        }
                    } else {
                        //Log.i("AmplifyQuery", "No data returned");
                        response.getErrors().forEach(error -> Log.i("AmplifyQuery", error.getMessage()));
                    }
                },
                failure -> Log.e("AmplifyQuery", failure.getMessage()));
    }

    private static GraphQLRequest<PaginatedResult<User>> getUserSearchContainsQuery(String neUserId, String username, int page_limit) {
        String document = "query searchUsers($filter:ModelUserFilterInput!,$limit:Int!){\n" +
                "  listUsers(filter:$filter, limit:$limit){\n" +
                "    items{\n" +
                "      userId\n" +
                "      username\n" +
                "    }\n" +
                "    nextToken\n" +
                "  }\n" +
                "}";

        HashMap<String,Object> filter = new HashMap<>();
        filter.put("userId",Collections.singletonMap("ne",neUserId));
        filter.put("username",Collections.singletonMap("contains",username));
        HashMap<String,Object> inputMap = new HashMap<>();
        inputMap.put("filter",filter);
        inputMap.put("limit",page_limit);
        return new SimpleGraphQLRequest<>(
                document,
                inputMap,
                PaginatedResult.class,
                new GsonVariablesSerializer()
        );
    }

    /**
     * Grab the user object by userId
     *
     * @param handler      result handler
     * @param responseCode
     */
    public static void QueryUserByUserId(Handler handler, int responseCode) {
        Amplify.API.query(ModelQuery.list(
                User.class,
                User.USER_ID.eq(Amplify.Auth.getCurrentUser().getUserId())),
                response -> {
                    if (response.getData() != null) {
                        handleServerResponse(
                                handler,
                                responseCode,
                                response.getData().getItems().iterator().next());
                    } else {
                        if (response.hasErrors()) {
                            for (GraphQLResponse.Error error : response.getErrors()) {
                                Log.e("AmplifyQuery", error.getMessage());
                            }
                        }
                    }
                }, failure -> Log.e("AmplifyQuery", failure.getCause().getMessage()));
    }

    /**
     * @param userId         Id of the user object we want to find
     * @param conversationId The conversation the current to which the user queried belongs.
     */
    public static void QueryUserByConversationId(Handler handler, int responseCode, String userId, String conversationId) {
        Amplify.API.query(ModelQuery.list(
                User.class,
                User.USER_ID.eq(userId)),
                response -> {
                    if (response.getData() != null) {
                        handleServerResponse(
                                handler,
                                responseCode,
                                new ChatWrapper(conversationId, response.getData().getItems().iterator().next()));
                    } else {
                        if (response.hasErrors()) {
                            response.getErrors().forEach(error -> Log.e("AmplifyQuery", error.getMessage()));
                        }
                    }
                },
                failure -> Log.e("AmplifyQuery", failure.getCause().getMessage()));
    }

    /**
     * Get the messages for a particular conversation
     *
     * @param conversationId
     */
    public static void QueryChatObject(Handler handler, int responseCode, String conversationId) {
        Amplify.API.query(
                ModelQuery.list(
                        DirectMessageChat.class,
                        DirectMessageChat.CONVERSATION_ID.eq(conversationId)),
                response -> {
                    //response.getData().
                    //need to handle all the messages per conversation
                    if (response.getData() != null) {
                        //now this should return a list of messages that may need to be continued to query if there are more than 20 items
                        //result is always size 1
                        //so now what now we grab the messages from direct message chat request and put those messages in the map
                        //we don't care about the object we just want the messages
                        handleServerResponse(
                                handler,
                                responseCode,
                                response.getData().getItems().iterator().next());
                    } else {
                        if (response.hasErrors()) {
                            response.getErrors().forEach(error -> {
                                Log.e("AmplifyQuery", error.getMessage());
                            });
                        }
                    }
                },
                failure -> {
                    Log.e("AmplifyQuery", failure.getCause().getMessage());
                });
    }

    /**
     * @param conversationId The conversation editor object we want to grab
     */
    public static void QueryChatEditors(Handler handler, int responseCode, String conversationId) {
        //only and we want to save the user object
        Amplify.API.query(ModelQuery.list(
                DirectMessageEditor.class,
                DirectMessageEditor.CONVERSATION_ID.eq(conversationId)),
                response -> {
                    if (response.getData() != null) {
                        //max 2 result
                        handleServerResponse(
                                handler,
                                responseCode,
                                response.getData().getItems());
                    } else {
                        if (response.hasErrors()) {
                            response.getErrors().forEach(error -> {
                                Log.e("AmplifyQuery", error.getMessage());
                            });
                        }
                    }
                },
                failure -> {
                    Log.e("AmplifyQuery", failure.getCause().getMessage());
                });
    }


    /**
     * Get friend requests that were sent to user while not in the app
     *
     * @param request On first call request should be null then request
     *                will be called recursively from within the response for more results than the page limit
     */
    public static void QueryFriendRequests(Handler handler, int responseCode, GraphQLRequest<PaginatedResult<FriendRequest>> request) {
        GraphQLRequest<PaginatedResult<FriendRequest>> getFriendRequests = request;
        if (getFriendRequests == null) {
            getFriendRequests = ModelQuery.list(
                    FriendRequest.class,
                    FriendRequest.RECIPIENT_ID.eq(Amplify.Auth.getCurrentUser().getUserId()));
        }
        Amplify.API.query(getFriendRequests,
                response -> {
                    if (response.hasData()) {
                        //Log.i("AmplifyQuery", "Queried friend requests");
                        PaginatedResult<FriendRequest> requests = response.getData();
                        handleServerResponse(handler, responseCode, requests);
                        if (requests.hasNextResult()) {
                            //if there are more friend requests
                            QueryFriendRequests(handler, responseCode, requests.getRequestForNextResult());
                        }
                    } else {
                        if (response.hasErrors()) {
                            for (GraphQLResponse.Error error : response.getErrors()) {
                                Log.e("AmplifyQuery", error.getMessage());
                            }
                        }
                    }
                },
                failure -> {
                    Log.e("AmplifyQuery", failure.getMessage());
                });
    }


    /**
     * Subscribe to invites for the currently signed in user
     */
    public static GraphQLOperation<FriendRequest> SubscribeToFriendRequests(Handler handler, int responseCode) {
        mFriendRequestSubscription = Amplify.API.subscribe(
                SnapchatAPI.getFriendRequestSubscriptionRequest(Amplify.Auth.getCurrentUser().getUserId()),
                onSubscriptionEstablished -> {
                    //Log.i("AmplifySubscribe", "FriendRequest Subscription established!")
                },
                onNextResponse -> {
                    //;
                    //Log.i("AmplifySubscribe", "Result->" + onNextResponse.getData().toString());
                    handleServerResponse(handler, responseCode, onNextResponse.getData());
                },
                onSubscriptionFailure -> Log.e("AmplifySubscribe", onSubscriptionFailure.getMessage()),
                () -> {}//Log.i("AmplifySubscribe", "FriendRequest Subscription completed!")
        );
        return mFriendRequestSubscription;
    }

    /**
     * Subscribe to new messages for the currently signed in user
     */
    public static GraphQLOperation<Message> SubscribeToMessages(Handler handler, int responseCode) {
        mMessageSubscription = Amplify.API.subscribe(
                SnapchatAPI.getMessageSubscriptionRequest(Amplify.Auth.getCurrentUser().getUserId()),
                onSubscriptionEstablished -> {},//Log.i("AmplifySubscribe", "Message Subscription established!"),
                onNextResponse -> {
                    //we need the chat fragment to handle the response
                    //Log.i("AmplifySubscribe", "Result->" + onNextResponse.getData().toString());
                    //need to handle the messages in the chat fragment
                    //if chat fragment is visible or in the stack then we can live update the message list
                    //mRequestResponseHandler.sendMessage(mRequestResponseHandler.obtainMessage())
                    handleServerResponse(handler, responseCode, onNextResponse.getData());
                },
                onSubscriptionFailure -> Log.e("AmplifySubscribe", onSubscriptionFailure.getMessage()),
                () -> {}//Log.i("AmplifySubscribe", "Message Subscription completed!")
        );
        return mMessageSubscription;
    }

    /**
     * Subscribe to new editors for the currently signed in user
     * editors are the objects that allow the user to edit conversations
     */
    public static GraphQLOperation<FriendRequestResponse> SubscribeToFriendRequestResponse(Handler handler, int responseCode) {
        mFriendRequestResponseSubscription = Amplify.API.subscribe(
                SnapchatAPI.getFriendRequestResponseSubscription(Amplify.Auth.getCurrentUser().getUserId()),//current user is the sender of the friendrequest
                onSubscriptionEstablished -> {},//Log.i("AmplifySubscribe", "FriendRequestResponse Subscription established!"),
                onNextResponse -> {
                    //we need the chat fragment to handle the response
                    //Log.i("AmplifySubscribe", "Result->" + onNextResponse.getData().toString());
                    //need to handle the messages in the chat fragment
                    //if chat fragment is visible or in the stack then we can live update the message list
                    //mRequestResponseHandler.sendMessage(mRequestResponseHandler.obtainMessage())
                    handleServerResponse(handler, responseCode, onNextResponse.getData());
                },
                onSubscriptionFailure -> Log.e("AmplifySubscribe", onSubscriptionFailure.getCause().getMessage()),
                () -> {}//Log.i("AmplifySubscribe", "FriendRequestResponse Subscription completed!")
        );
        return mFriendRequestResponseSubscription;
    }

    private static GraphQLRequest<Message> getMessageSubscriptionRequest(String recipientId) {
        //return null;
        String document =
                "subscription getMessages($recipientId: ID!) {\n" +
                        "  onCreateMessageForRecipient(recipientId: $recipientId) {\n" +
                        "    id\n" +
                        "    authorId\n" +
                        "    recipientId\n" +
                        "    conversationId\n" +
                        "    content\n" +
                        "    createdAt\n" +
                        "    isSnap\n" +
                        "    bucket\n" +
                        "    region\n" +
                        "    key\n" +
                        "    unread\n" +
                        "  }\n" +
                        "}";

        return new SimpleGraphQLRequest<>(
                document,
                Collections.singletonMap("recipientId", recipientId),
                Message.class,
                new GsonVariablesSerializer());
    }

    private static GraphQLRequest<FriendRequestResponse> getFriendRequestResponseSubscription(String requestSenderId) {
        //return null;
        String document =
                "subscription getFriendRequestResponses($requestSenderId:ID!) {\n" +
                        "  onCreateFriendRequestResponseForSelf(requestSenderId:$requestSenderId) {\n" +
                        "    id\n" +
                        "    requestSenderId\n" +
                        "    accepted\n" +
                        "    conversationId\n" + //get the conversationId
                        "  }\n" +
                        "}";

        return new SimpleGraphQLRequest<>(
                document,
                Collections.singletonMap("requestSenderId", requestSenderId),
                FriendRequestResponse.class,
                new GsonVariablesSerializer());
    }

    private static GraphQLRequest<FriendRequest> getFriendRequestSubscriptionRequest(String recipientId) {
        String document =
                //WHEN TESTING FROM CONSOLE ONLY THE FIELDS RETURNED IN THE QUERY SHOULD BE SHOWN BELOW
                "subscription getFriendRequests($recipientId: ID!) {\n" +
                        "  onCreateFriendRequestForRecipient(recipientId: $recipientId) {\n" +
                        "    id\n" +
                        "    authorId\n" +
                        "    authorUsername\n" +
                        "    recipientId\n" +
                        "  }\n" +
                        "}";
        return new SimpleGraphQLRequest<>(
                document
                , Collections.singletonMap("recipientId", recipientId)
                , FriendRequest.class
                , new GsonVariablesSerializer()
        );
    }

    private static GraphQLRequest<Message> getSendMessageRequest(String authorId, String recipientId, String conversationId, String content) {

        String document = "mutation CreateMessage(\n" +
                "  $input: CreateMessageInput!\n" +
                ") {\n" +
                "  createMessage(input: $input) {\n" +
                "    authorId\n" +
                "    recipientId\n" +
                "    conversationId\n" +
                "    content\n" +
                "    createdAt\n" +
                "    isSnap\n" +
                "    bucket\n" +
                "    region\n" +
                "    key\n" +
                "    unread\n" +
                "  }\n" +
                "}";

        HashMap<String, Object> input = new HashMap<>();
        input.put("authorId", authorId);
        input.put("recipientId", recipientId);
        input.put("conversationId", conversationId);
        input.put("content", content);
        input.put("isSnap",false);

        HashMap<String, Object> inputMap = new HashMap<>();
        inputMap.put("input", input);
        return new SimpleGraphQLRequest<>(
                document,
                inputMap,
                Message.class,
                new GsonVariablesSerializer());
    }

    public static void SendMessage(Handler handler, int responseCode, String authorId, String recipientId, String conversationId, String content) {
        Amplify.API.mutate(SnapchatAPI.getSendMessageRequest(authorId, recipientId, conversationId, content),
                response -> {
                    if (response.getData() != null) {
                        //Log.i("AmplifyMutate", "Message sent successfully");
                        handleServerResponse(handler, responseCode, response.getData());
                    } else {
                        if (response.hasErrors()) {
                            response.getErrors().forEach(error -> {
                                Log.e("AmplifyMutate", error.getMessage());
                            });
                        }
                    }
                },
                failure -> {
                    Log.e("AmplifyMutate", failure.getCause().getMessage());
                }
        );
    }

    public static void UnSubscribe() {
        UnsubscribeToMessages();
        UnsubscribeToFriendRequests();
        UnsubscribeToFriendRequestResponse();
    }

    public static void UnsubscribeToFriendRequests() {

        if (mFriendRequestSubscription != null) {
            mFriendRequestSubscription.cancel();
            mFriendRequestSubscription = null;
        }
    }

    private static void UnsubscribeToFriendRequestResponse() {

        if (mFriendRequestResponseSubscription != null) {
            mFriendRequestResponseSubscription.cancel();
            mFriendRequestResponseSubscription = null;
        }
    }

    private static void UnsubscribeToMessages() {
        if (mMessageSubscription != null) {
            mMessageSubscription.cancel();
            mMessageSubscription = null;
        }
    }

    /**
     * Send an friendrequestresponse object back to the server accepting the request from requestSender
     *
     * @param handler
     * @param responseCode
     * @param accept
     */
    public static void SendFriendRequestResponse(Handler handler, int responseCode, FriendRequest friendRequest, boolean accept) {

        GraphQLRequest<FriendRequestResponse> mutation = accept ? getAcceptFriendRequestMutation(friendRequest.getAuthorId()) : getDeclineFriendRequestMutation(friendRequest.getAuthorId());
        Amplify.API.mutate(mutation,
                response -> {
                    //sent friend request
                    handleServerResponse(handler, responseCode, response.getData());
                },
                failure -> Log.e("AmplifyMutate", failure.getMessage()));
        //delete the friend request object so we don't query it again
        Amplify.API.query(getDeleteFriendRequestQuery(friendRequest.getId()),
                response -> {
                    if (response.hasErrors()) {
                        for (GraphQLResponse.Error error : response.getErrors()) {
                            Log.e("AmplifyMutate", error.getMessage());
                        }
                    } else {
                       // Log.i("AmplifyMutate", "Deleted friend request!");
                    }

                },
                failure -> Log.e("AmplifyMutate", failure.getMessage())
        );
    }

    private static GraphQLRequest<String> getDeleteFriendRequestQuery(String id) {
        String document = "query removeFriendRequest($id:ID!){" +
                "onDeleteFriendRequest(id:$id)\n" +
                "}";
        return new SimpleGraphQLRequest<>(
                document,
                Collections.singletonMap("id", id),
                String.class,
                new GsonVariablesSerializer()
        );
    }

    private static GraphQLRequest<FriendRequestResponse> getDeclineFriendRequestMutation(String requestSenderId) {
        String document = "mutation declineFriendRequest($requestSenderId: ID!) {\n" +
                "  onDeclineFriendRequest(requestSenderId: $requestSenderId) {\n" +
                "    id\n" +
                "    requestSenderId\n" +
                "    accepted\n" +
                "  }\n" +
                "}";
        return new SimpleGraphQLRequest<>(
                document,
                Collections.singletonMap("requestSenderId", requestSenderId),
                FriendRequestResponse.class,
                new GsonVariablesSerializer()
        );
    }

    private static GraphQLRequest<FriendRequestResponse> getAcceptFriendRequestMutation(String requestSenderId) {
        String document = "mutation acceptFriendRequest($requestSenderId: ID!) {\n" +
                "  onAcceptFriendRequest(requestSenderId: $requestSenderId) {\n" +
                "    id\n" +
                "    requestSenderId\n" +
                "    accepted\n" +
                "    conversationId\n" +
                "  }\n" +
                "}";
        return new SimpleGraphQLRequest<>(
                document,
                Collections.singletonMap("requestSenderId", requestSenderId),
                FriendRequestResponse.class,
                new GsonVariablesSerializer()
        );
    }

    public static GraphQLRequest<FriendRequest> getCreateFriendRequest(String recipientId, String authorUsername) {
        String document = "mutation CreateFriendRequest(\n $input: CreateFriendRequestInput!\n) {\n" +
                "  createFriendRequest(input: $input) {\n" +
                "    id\n" +
                "    authorId\n" +
                "    authorUsername\n" +
                "    recipientId\n" +
                "  }\n" +
                "}";

        HashMap<String, Object> input_map = new HashMap<>();
        input_map.put("authorId", Amplify.Auth.getCurrentUser().getUserId());
        input_map.put("authorUsername", authorUsername);
        input_map.put("recipientId", recipientId);
        HashMap<String, Object> input = new HashMap<>();
        input.put("input", input_map);
        return new SimpleGraphQLRequest<>(
                document,
                input,
                FriendRequest.class,
                new GsonVariablesSerializer()
        );
    }

    public static void DeleteFriendRequestResponse(FriendRequestResponse response) {
        Amplify.API.mutate(ModelMutation.delete(response),
                success -> {
                    if (success.hasErrors()) {
                        for (GraphQLResponse.Error error : success.getErrors()) {
                            Log.e("AmplifyMutate", error.getMessage());
                        }
                    } else {
                        //Log.i("AmplifyMutate", "DeletedFriendRequestResponse");
                    }
                }, failure -> {
                    Log.e("AmplifyMutate", failure.getMessage());
                });
    }

    public static void SendSnap(Handler handler, int responseCode, File file, String authorId, String recipientId, String conversationId) {
        UUID id = UUID.randomUUID();//random file id
        Amplify.Storage.uploadFile(
                recipientId + "/" + conversationId + "/" + id.toString(),//some uuid
                file,
                result -> {
                    //Log.i("AmplifyStorage", "Successfully uploaded: " + result.getKey());
                    //create a snapobject needs to be done on the backend but too long to wrestle with documentation
                    //new s3object
                    //new snap object
                    //or i should have recipient  folders
                    //with subfolders of conversation
                    //and the each next name
                    file.delete();
                    Message snap = Message.builder()
                            .authorId(authorId)
                            .recipientId(recipientId)
                            .conversationId(conversationId)
                            .isSnap(true)
                            .bucket("snap60imagebucket124041-scdev")
                            .region("us-east-1")
                            .key(result.getKey())
                            .unread(true)
                            .id(id.toString())
                            .build();//use this id in conjunction with conversation and recipient
                    SendSnapNotification(handler,responseCode,snap);
                },
                storageFailure -> {
                    //handleServerResponse(handler, responseCode);
                    Log.e("AmplifyStorage", "Upload failed", storageFailure);
                }
        );
    }

    private static void SendSnapNotification(Handler handler, int responseCode, Message snap) {
        Amplify.API.mutate(ModelMutation.create(snap),
                result -> {
                    if (result.hasErrors()) {
                        for (GraphQLResponse.Error error : result.getErrors()) {
                            Log.e("AmplifyMutate", error.getMessage());
                        }
                    } else {
                        //Log.i("AmplifyMutate", "Sent SNAP!");
                        //catch snaps with self as recipient id
                        handleServerResponse(handler,responseCode,result.getData());
                    }
                }, failure -> {
                    Log.e("AmplifyMutate", failure.getMessage());
                });
    }

    public static void DownloadImage(Handler handler, int responseCode,  SnapWrapper wrapper) {

        Amplify.Storage.downloadFile(wrapper.snap.getKey(), wrapper.image_file,
                success -> {
                    handleServerResponse(handler, responseCode,wrapper);
                },
                error -> {
                    Log.e("AmplifyStorage", error.getLocalizedMessage());
                });
    }

    //mark the snap as read so it won't get queried again
    public static void UpdateSnap(Message snap) {
        Amplify.API.mutate(ModelMutation.update(snap.copyOfBuilder()
                .unread(false).build()),
                response->{
                    if(response.hasErrors()){
                        for (GraphQLResponse.Error error : response.getErrors()) {
                            Log.e("AmplifyMutate",error.getMessage());
                        }
                    }/*else{
                        Log.i("AmplifyMutate","unread => "+response.getData().getUnread());
                    }*/
                },
                failure->{
                    Log.e("AmplifyMutate",failure.getMessage());
                });
    }



      /*
    TESTS FOR FIELD LEVEL AUTHORIZATION

    public void testFieldLevelAuthorization(){
        Amplify.API.query(ModelQuery.list(Todo.class),
                response->{
                    if(response.getData() != null) {
                        //this is a single object query
                        //handleUserQueryResponse(USER_QUERY_RESULT, response.getData().getItems().iterator().next());
                        Iterable<Todo> todos = response.getData().getItems();
                        todos.iterator().forEachRemaining( todo ->{
                            //we should not be able to get the private field of the todo that doesnt belong to us
                            //worked :)
                            Log.i("AmplifyQuery","todo.userId=>"+todo.getUserId());
                            Log.i("AmplifyQuery","privateField => "+todo.getPrivateField());
                        });
                    }else{
                        if(response.hasErrors()){
                            for (GraphQLResponse.Error error : response.getErrors()) {
                                Log.e("AmplifyQuery",error.getMessage());
                            }
                        }
                    }
                },
                failure->{

                });
    }
    public void testFieldLevelAuthWithoutTypeLevelAuth(){
        Amplify.API.query(ModelQuery.list(Event.class),
                response->{
                    if(response.getData() != null) {
                        //this is a single object query
                        //handleUserQueryResponse(USER_QUERY_RESULT, response.getData().getItems().iterator().next());
                        Iterable<Event> events = response.getData().getItems();
                        events.iterator().forEachRemaining( event ->{
                            //we should not be able to get the private field of the todo that doesnt belong to us
                            //worked :)
                            Log.i("AmplifyQuery","event.userId=>"+event.getUserId());
                            Log.i("AmplifyQuery","privateField => "+event.getPrivateField());
                        });
                    }else{
                        if(response.hasErrors()){
                            for (GraphQLResponse.Error error : response.getErrors()) {
                                Log.e("AmplifyQuery",error.getMessage());
                            }
                        }
                    }
                },
                failure->{

                });
    }*/
}
