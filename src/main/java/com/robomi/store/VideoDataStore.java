package com.robomi.store;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class VideoDataStore {
        private BlockingQueue<byte[]> videoFrameQ;

        public VideoDataStore() {
                // LinkedBlockingQueue로 videoFrameQ를 초기화
                this.videoFrameQ = new LinkedBlockingQueue<>();
        }

        public void insertQueue(byte[] frame){
                try{
                        videoFrameQ.put(frame);
                } catch(Exception e){
                        e.printStackTrace();
                }
        }

        public byte[] potQueue(){
                try{
                        return videoFrameQ.take();

                } catch (Exception e){
                        e.printStackTrace();
                        return null;
                }
        }
}
