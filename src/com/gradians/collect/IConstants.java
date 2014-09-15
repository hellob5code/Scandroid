package com.gradians.collect;

public interface IConstants {
    
    String WEB_APP_HOST_PORT = "192.168.1.9:3000";
    String BANK_HOST_PORT = "109.74.201.62:8080";
    
    String TAG = "com.gradians.collect";
    String TAG_ID = "com.gradians.collect.ID";
    String TOKEN_KEY = "token";
    String NAME_KEY = "name";
    String EMAIL_KEY = "email";
    String DIR_KEY = "dir";
    String QOTD_KEY = "qotd";
    String COUNT_KEY = "count";
    
    String TOPICS_KEY = "topics";
    String VERT_ID_KEY = "v_id";
    String VERT_NAME_KEY = "v_name";
    
    String ITEMS_KEY = "ws";
    String MARKER_KEY = "marker";
    String QUIZ_ID_KEY = "quizId";
    String QUIZ_NAME_KEY = "quiz";
    String QUIZ_PATH_KEY = "locn";
    String QUIZ_PRICE_KEY = "price";
    String QUIZ_LAYOUT_KEY = "layout";
    String QUIZ_FDBK_KEY = "fdbkMrkr";
    String QUESTIONS_KEY = "questions";
    
    String PZL_TYPE = "PZL";
    String QSN_TYPE = "QSN";
    String GR_TYPE = "GR";
    String QR_TYPE = "QR";
    
    String COMMENTS_KEY = "comments";
    String COMMENT_KEY = "comment";
    String X_POSN_KEY = "x";
    String Y_POSN_KEY = "y";
    
    String STATE_KEY = "state";
    String ID_KEY = "id";
    String QUESN_ID_KEY = "qid";
    String SBPRTS_ID_KEY = "sid";
    String PZL_KEY = "pzl";
    String GR_ID_KEY = "grId";
    String IMG_PATH_KEY = "img";
    String SCAN_KEY = "scan";
    String SCANS_KEY = "scans";
    String IMG_SPAN_KEY = "imgspan";
    String MARKS_KEY = "marks";
    String OUT_OF_KEY = "outof";
    String EXAMINER_KEY = "examiner";
    String FDBK_MRKR_KEY = "fdbkMrkr";
    String HINT_MRKR_KEY = "hintMrkr";
    
    // Directory names
    String QUESTIONS_DIR_NAME = "questions";
    String ANSWERS_DIR_NAME = "answers";
    String SOLUTIONS_DIR_NAME = "solutions";
    String FEEDBACK_DIR_NAME = "feedback";
    String UPLOAD_DIR_NAME = "upload";
    String HINTS_DIR_NAME = "hints";
    
    String STATE_FILE = "state.txt";
    
    // States: Question
    short LOCKED = 0,
          DOWNLOADED = 1,
          CAPTURED = 2,
          SENT = 3,
          RECEIVED = 4,
          GRADED = 5;
    
    // States: Quiz
    short NOT_YET_BILLED = 0,
          NOT_YET_STARTED = 1,
          NOT_YET_COMPLETED = 2,
          NOT_YET_SENT = 3,
          NOT_YET_GRADED = 4,
          DONE = 5;
}
