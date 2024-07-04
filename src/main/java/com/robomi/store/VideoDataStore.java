package com.robomi.store;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class VideoDataStore {
        private BlockingQueue<byte[]> objVideoFrameQ;
        private BlockingQueue<byte[]> rtVideoFrameQ;

        public VideoDataStore() {
                // LinkedBlockingQueue로 videoFrameQ를 초기화
                this.objVideoFrameQ = new LinkedBlockingQueue<>();
                this.rtVideoFrameQ = new LinkedBlockingQueue<>();
        }

        public void insertObjQueue(byte[] frame){
                try{
                        objVideoFrameQ.put(frame);
                } catch(Exception e){
                        e.printStackTrace();
                }
        }
        public void insertRtQueue(byte[] frame){
                try{
                        rtVideoFrameQ.put(frame);
                } catch(Exception e){
                        e.printStackTrace();
                }
        }

        public byte[] popObjQueue(){
                try{
                        return objVideoFrameQ.take();

                } catch (Exception e){
                        e.printStackTrace();
                        return null;
                }
        }
        public byte[] popRtQueue(){
                try{
                        return rtVideoFrameQ.take();

                } catch (Exception e){
                        e.printStackTrace();
                        return null;
                }
        }
}
