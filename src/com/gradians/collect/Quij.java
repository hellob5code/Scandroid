package com.gradians.collect;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Using it as a collection of questions based on any creiterea. 
 * Need not be a formal Quiz given by an instructor to students.
 * 
 * @author adamarla
 */
class Quij extends ArrayList<Question> implements Parcelable, Comparable<Quij>, JSONAware, IConstants {
    
    public Quij(String name, String path, long id, int price, String type, String layout) {
        this.name = name;
        this.id = id;
        this.path = path;
        this.price = price;
        this.type = type;
        this.layout = layout;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDisplayTotal() {
        String display = "";
        int touched = 0;
        Question[] questions = this.getQuestions();
        for (Question q : questions)
            if (q.getState() > DOWNLOADED ||
                q.tried() || q.botAnswer() ||
                q.botSolution()) 
                touched++;
        
        display = String.format("%2d / %2d", touched, this.size());
        if (getState() == GRADED && !type.equals(QSN_TYPE)) {
            display = String.format("%2d%%",
                (int)(this.getScore()*100/this.getMax()));
        }
        return display;
    }
    
    public void determineState() {
        if (this.get(0).getGRId()[0] == 0) {
            state = NOT_YET_BILLED;
            return;
        }
        
        short leastFarAlong = this.get(this.size()-1).getState();
        for (Question q : this) {
            leastFarAlong = q.getState() < leastFarAlong ?
                q.getState() : leastFarAlong;
        }
        
        switch (leastFarAlong) {
        case LOCKED:
        case DOWNLOADED:
            state = NOT_YET_COMPLETED;
            break;
        case CAPTURED:
            state = NOT_YET_SENT;
            break;
        case SENT:
        case RECEIVED:
            state = NOT_YET_GRADED;
            break;
        case GRADED:
            state = GRADED;
        }        
    }
    
    public short getState() {
        return state;
    }
    
    public String getName() {
        return name;
    }
    
    public long getId() {
        return id;
    }
    
    public String getPath() {
        return path;
    }
    
    public int getPrice() {
        return price;
    }
    
    public String getType() {
        return type;
    }
    
    public double getScore() {
        double total = 0;
        for (Question q : this)
            total += q.getMarks();
        return total;
    }
    
    public short getMax() {
        short max = 0;
        for (Question q : this)
            max += q.getOutOf();
        return max;
    }
    
    public String getLayout() {
        return layout;
    }
    
    public Question[] getQuestions() {
        return this.toArray(new Question[this.size()]);
    }
    
    public Question[] getQuestionsByState(int state) {
        ArrayList<Question> filtered = new ArrayList<Question>();
        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).getState() == state) {
                filtered.add(this.get(i));
            }
        }
        return filtered.toArray(new Question[filtered.size()]);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Parcelable.Creator<Quij> CREATOR
        = new Parcelable.Creator<Quij>() {

            @Override
            public Quij createFromParcel(Parcel source) {
                return new Quij(source);
            }

            @Override
            public Quij[] newArray(int size) {
                return new Quij[size];
            }
    };
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(path);
        dest.writeString(layout);
        dest.writeLong(id);
        dest.writeInt(state);
        dest.writeInt(price);
        dest.writeString(type);
        dest.writeTypedList(this);
    }
    
    private Quij(Parcel in) {
        name = in.readString();
        path = in.readString();
        layout = in.readString();
        id = in.readLong();
        state = (short)in.readInt();
        price = in.readInt();
        type = in.readString();
        this.addAll(in.createTypedArrayList(Question.CREATOR));
    }
    
    @Override
    public int compareTo(Quij another) {
        return getName().compareTo(another.getName());
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    private int price;
    private String type;
    private String name, path, layout;
    private long id;
    private short state;
    
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    public String toJSONString() {
        JSONObject obj = new JSONObject();
        obj.put(QUIZ_NAME_KEY, name);
        obj.put(QUIZ_PATH_KEY, path);
        obj.put(QUIZ_ID_KEY, id);
        obj.put(QUIZ_PRICE_KEY, price);
        obj.put(QUIZ_LAYOUT_KEY, layout);
        
        JSONArray array = new JSONArray();
        for (Question q : this) {
            array.add(q);
        }
        obj.put(QUESTIONS_KEY, array);
        return obj.toString();
    }

}