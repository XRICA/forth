import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.RuoYiApplication;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.monitor.Vo.MsgCountVo;
import com.ruoyi.monitor.Vo.PredictDataVo;
import com.ruoyi.monitor.domain.*;
import com.ruoyi.monitor.service.*;
import com.ruoyi.monitor.ExcelTest;
import com.ruoyi.monitor.mapper.PollutantAvgMapper;
import com.ruoyi.monitor.utils.Common.Constant;
import com.ruoyi.monitor.utils.Common.DateUtils;
import com.ruoyi.monitor.utils.Email.MailCenter;
import com.ruoyi.monitor.utils.PythonServer.PyConnect;
import com.ruoyi.monitor.utils.PythonServer.PyConstant;
import com.ruoyi.monitor.utils.PythonServer.predict.PreUtils;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysUserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = RuoYiApplication.class)
@RunWith(SpringRunner.class)
public class MyTest {
    @Autowired
    private IPollutantService service;
    @Autowired
    private IPolluantAvgService avgService;
    @Autowired
    private PollutantAvgMapper avgMapper;
    @Autowired
    private MailCenter mailCenter;
    @Autowired
    private ISysMsgModelService sysEmailModelService;

    @Test
    public void test1() {
        SysUser user = new SysUser();
        SysUser user1 = new SysUser();
        user.setEmail("2423631557@qq.com");
        user1.setEmail("xuzexuan2000327001@163.com");
        SysUser[] users = {user,user1};
        String mails = "";
        for (int i = 0; i < users.length; i++) {
            mails+=(users[i].getEmail());
            if (i<users.length-1){
                mails+=(";");
            }
        }
        mailCenter.sendTo(users);
    }



    @Test
    public void one(){
        List<Pollutant> pollutantList = redisCache.getCacheList("pollutantList");
        System.out.println(pollutantList);
    }

    @Test
    public void week(){
        List<PollutantAvg> list = redisCache.getCacheList("radar");
        System.out.println(list.size());
        if (list.size() == 0){
            PollutantAvg data30 = avgService.selectDaysAVG(new Date(), 30);
            System.out.println(data30);
            PollutantAvg data7 = avgService.selectDaysAVG(new Date(), 7);
            System.out.println(data7);
            PollutantAvg today = avgService.selectDaysAVG(new Date(), 0);
            System.out.println(today);
        }

    }

    /**
     * 读取excel文件
     */
    @Test
    public void saveBatch(){
        String[] paths = {"C:\\Users\\徐泽炫\\Desktop\\上海\\2021","C:\\Users\\徐泽炫\\Desktop\\上海\\2022"};
        List<Pollutant> pollutantList = ExcelTest.findAll(paths, new ArrayList<Pollutant>());

        System.out.println(service.saveBatch(pollutantList));
        System.out.println(pollutantList.size());
    }

    @Test
    public void likeDate(){
//        PollutantAvg pollutantAvg = avgService.selectOneDayAVG(new Date());
//        System.out.println(pollutantAvg);
        PollutantAvg pollutantAvgs = avgService.selectDaysAVG(new Date(), 3);
        System.out.println(pollutantAvgs);
//        avgService.
    }
    //均值插入测试
    SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Test
    public void avgInsertD(){

        Pollutant avgDay = service.CountAVGDay();
        System.out.println(avgDay);
        PollutantAvg avg = new PollutantAvg();
        BeanUtils.copyProperties(avgDay,avg);
        avg.setId(null);
        Calendar instance = Calendar.getInstance();
        DateTime beginOfDay = DateUtil.beginOfDay(avgDay.getCt());
        avg.setStart(beginOfDay);
        DateTime endOfDay = DateUtil.endOfDay(avgDay.getCt());
        instance.setTime(endOfDay);
        instance.set(Calendar.MILLISECOND,0);
        avg.setEnd(instance.getTime());
        System.out.println("目标均值："+avg);
        avg.setType(Constant.DAILY);
        avgService.saveOrUpdate(avg,new UpdateWrapper<PollutantAvg>().apply("start = "+ "'"+sim.format(beginOfDay)+ "'")
                .apply("end = "+ "'"+sim.format(endOfDay)+ "'"));

    }

    /**
     * 均值插入
     */
    @Test
    public void Insert(){
        avgInsertDAll();
        avgInsertWAll();
        avgInsertMAll();
        avgInsertYAll();
    }
    @Test
    public void avgInsertDAll(){
        List<PollutantAvg> res = new ArrayList<>();
        Set<Pollutant> pollutants = service.CountAVGDayAll();
        for (Pollutant pollutant:pollutants) {
//            System.out.println(pollutant);
            PollutantAvg avg = new PollutantAvg();
            BeanUtils.copyProperties(pollutant,avg);
            avg.setId(null);
            Calendar instance = Calendar.getInstance();
            DateTime beginOfDay = DateUtil.beginOfDay(pollutant.getCt());
            avg.setStart(beginOfDay);
            DateTime endOfDay = DateUtil.endOfDay(pollutant.getCt());
            instance.setTime(endOfDay);
            instance.set(Calendar.MILLISECOND,0);
            avg.setEnd(instance.getTime());
            System.out.println(pollutant.getCt().getDate()+":目标均值-："+avg);
            avg.setType(Constant.DAILY);
            res.add(avg);
        }
        avgService.saveOrUpdateBatch(res);

    }

    //均值插入测试
    @Test
    public void avgInsertW(){
        Pollutant avgWeek = service.CountAVGWeek();
        System.out.println(avgWeek);
        PollutantAvg avg = new PollutantAvg();
        BeanUtils.copyProperties(avgWeek,avg);
        avg.setId(null);
        //去除毫秒
        Calendar instance = Calendar.getInstance();

        DateTime beginOfWeek = DateUtil.beginOfWeek(avgWeek.getCt());
        avg.setStart(beginOfWeek);

        DateTime endOfWeek = DateUtil.endOfWeek(avgWeek.getCt());
        instance.setTime(endOfWeek);
        instance.set(Calendar.MILLISECOND,0);
        avg.setEnd(instance.getTime());


        System.out.println("周标均值："+avg);
        avg.setType(Constant.WEEKLY);

        avgService.saveOrUpdate(avg,new UpdateWrapper<PollutantAvg>().apply("start = "+ "'"+sim.format(beginOfWeek)+ "'")
                .apply("end = "+ "'"+sim.format(endOfWeek)+ "'")
               );

    }



    @Test
    public void avgInsertWAll(){
        Set<PollutantAvg> pollutantAvgs = avgService.CountAVGWeekAll();
        avgService.saveOrUpdateBatch(pollutantAvgs);

    }
    //均值插入测试
    @Test
    public void avgInsertM(){
        Pollutant avgMonth = service.CountAVGMonth();
        System.out.println(avgMonth);
        PollutantAvg avg = new PollutantAvg();
        BeanUtils.copyProperties(avgMonth,avg);
        avg.setId(null);
        DateTime beginOfMonth = DateUtil.beginOfMonth(avgMonth.getCt());
        avg.setStart(beginOfMonth);

        Calendar instance = Calendar.getInstance();
        DateTime endOfMonth = DateUtil.endOfMonth(avgMonth.getCt());
        instance.setTime(endOfMonth);
        instance.set(Calendar.MILLISECOND,0);
        avg.setEnd(instance.getTime());
        System.out.println("月标均值："+avg);
        avg.setType(Constant.MONTHLY);
        avgService.saveOrUpdate(avg,new UpdateWrapper<PollutantAvg>().apply("start = "+ "'"+sim.format(beginOfMonth)+ "'")
                .apply("end = "+ "'"+sim.format(endOfMonth)+ "'"));
    }
    @Test
    public void avgInsertMAll(){
        Set<PollutantAvg> pollutantAvgs = avgService.CountAVGMonthAll();
        avgService.saveOrUpdateBatch(pollutantAvgs);

    }

    @Test
    public void avgInsertYAll(){
        Set<PollutantAvg> pollutantAvgs = avgService.CountAVGYearAll();
//        pollutantAvgs.forEach(avg->{
//
//            avgService.saveOrUpdate(avg,new UpdateWrapper<PollutantAvg>().apply("start = "+ "'"+sim.format(avg.getStart())+ "'")
//                    .apply("end = "+ "'"+sim.format(DateUtils.get0MIll(avg.getEnd()))+ "'"));
//        });
        avgService.saveOrUpdateBatch(pollutantAvgs);

    }
    @Test
    public void selectByType(){
//        List<PollutantAvg> pollutantAvgs = avgService.selectAVGByType(1);
//        System.out.println(DateUtil.beginOfDay(pollutantAvgs.get(pollutantAvgs.size()-1).getCt()).equals(
//                DateUtil.beginOfDay(new Date())
//        ));
        Set<PollutantAvg> avg = avgService.selectAVGByType(3);
        System.out.println(avg);
    }

    @Test
    public void testRadr(){
        System.out.println("--------------历史数据--------------");
        avgService.selectDaySingle(new Date(),31).forEach(data-> System.out.println(data));
        System.out.println("-----------------------------------");
    }

    @Test
    public void listAll(){
//        List<Pollutant> pollutants = service.selectPollutantListCondition(null);
//        System.out.println(pollutants);
        Pollutant pollutant = new Pollutant();
        pollutant.setCt(new Date());
        List<Pollutant> pollutants = service.selectPollutantListCondition(pollutant);
        pollutants.forEach(data-> System.out.println(data));
    }

    @Test
    public void countDays(){
//        TreeMap<String, HashMap<Integer, Integer>> map = avgService.daysOfQualities(false);

//        System.out.println(map);
        Pollutant pollutant = service.selectLatestPollutant();
        System.out.println(pollutant);
    }
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private ISysMessageService sysMessageService;
     @Test
    public void sysUser(){
         List<SysMessage> list = sysMessageService.selectSysMessageList(null);
         list.forEach(msg ->{
             SysUser sysUser = sysUserService.selectUserById(msg.getUserId());
             msg.setUser(sysUser);
         });
//         SysUser sysUser = sysUserService.selectUserById((long) 1);
         System.out.println(list);
     }

     @Autowired
     private ISysMsgService sysEmailService;
     @Test
    public void countMails(){
         List<MsgCountVo> msgCountVos = sysEmailService.countMsgs("2");
         System.out.println("***********************");
         System.out.println(msgCountVos);
         System.out.println("***********************");

     }

     @Test
     public void countAlarm(){
         List<MsgCountVo> msgCountVos = sysEmailService.countAlarm("3");
         System.out.println("***********************");
         System.out.println(msgCountVos);
         System.out.println("***********************");
     }

     @Autowired
     private ISysMenuService menuService;
     @Test
     public void findAuto(){
         SysMenu menu = menuService.selectFunctionByName();
         System.out.println("0".equals(menu.getStatus()));
         System.out.println(menu);
     }

    public double[] getTrainList(){
         Integer limit = 18000;
        List<Pollutant> pollutants = service.trainList(limit);
        double[] nums = new double[limit];
        for (int i = pollutants.size()-1; i >=0 ; i--) {
            System.out.println(pollutants.get(i).getCt());
            nums[i] = pollutants.get(i).getCo();
//            System.out.println(nums[i]);
        }
        return nums;
    }

    @Autowired
    private PyConnect pyConnect;
    /**
     * 数组形式
     */
    @Test
    public void pythonServerTrain(){
        JSONObject jsonObject = new JSONObject();
//        String model = "D:\\PyCharm Community Edition 2021.1.3\\new_lstm_stock\\tot.h5";
        String model = "D:\\PyCharm Community Edition 2021.1.3\\new_lstm_stock\\model\\preCo.h5";
        double[] train = getTrainList();
        jsonObject.putOpt("model_path",model);
        jsonObject.putOpt("nums",train);
        jsonObject.put("steps",120);
//        pyConnect.getConnection("http://localhost:5000/trainDefine", jsonObject, "POST");
        pyConnect.getConnection("http://localhost:5000/predict", jsonObject, "POST");
        pyConnect.write(jsonObject);
        pyConnect.getResult();


    }

     @Test
    public void pythonServer(){
         String model = "D:\\PyCharm Community Edition 2021.1.3\\new_lstm_stock\\gpt24.h5";
         JSONObject jsonObject = new JSONObject();
         jsonObject.putOpt("model",model);
         try {
             URL url = new URL("http://localhost:5000/predict");

             HttpURLConnection con = (HttpURLConnection) url.openConnection();
             con.setRequestProperty("Content-Type",  "application/json;charset=UTF-8");
             con.setRequestProperty("Content-Length", String.valueOf(jsonObject.toString().getBytes().length));
             con.setRequestMethod("POST");
             // post请求，参数要放在http正文内，因此需要设为true, 默认情况下是false;
             con.setDoOutput(true);
             // 设置是否从httpcon读入，默认情况下是true;
             con.setDoInput(true);
             // Post 请求不能使用缓存
             con.setUseCaches(false);

             OutputStream out = con.getOutputStream();

             out.write(jsonObject.toString().getBytes());
             out.flush();
             out.close();

             InputStream inputStream = con.getInputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
             String inputLine;
             StringBuilder content = new StringBuilder();
             while ((inputLine = in.readLine()) != null) {
                 content.append(inputLine);
             }
             in.close();
             String c = content.toString();
             System.out.println(c);
             if (c.contains("[")&&c.contains("]")){
                 String replaceAll = c.replaceAll("\\[", "").replaceAll("\\]", "");
                 String[] split = replaceAll.split(" ");
//            System.out.println(split.toString());
                 Long[] nums = new Long[split.length];
                 try {
                     for (int i = 0; i < split.length; i++) {
                         Long b = new Double(Double.parseDouble(split[i])).longValue();
                         nums[i] = b;
                         System.out.println(nums[i]);
                     }
//                        System.out.println(nums);
                 }catch (Exception e){
                     System.out.println("数据非数组");
                 }

             }


         }catch (Exception e) {
             e.printStackTrace();
         }
     }

     @Autowired
     private IPreModelService preModelService;

     @Test
    public void findByName(){
         PreModel aDefault = preModelService.selectPreModelByName("DEFAULT");
         System.out.println(aDefault);
     }
    @Autowired
    private RedisCache redisCache;
     @Test
    public void getPreDa(){
        redisCache.getCacheObject("24pre");

     }

     @Test
    public void getFromNow(){
         DateUtils.getFromNow(new Date(), 12);
     }

   @Autowired
   private PreUtils preUtils;
    @Test
    public void getAll(){
//        preUtils.singlePredict();
        Map<String, Object> map = redisCache.getCacheMap("pm10");

        System.out.println(new TreeMap<>(map));


    }

     @Test
    public void mixPre(){
         Map<String, Object> map = null;
         List<Integer> result = null;
         map = redisCache.getCacheMap("preMonth30*3");
         System.out.println(map);
         if (ObjectUtils.isEmpty(map)){
             PreModel model = preModelService.selectPreModelByName("DEFAULT");
             String modelPath = model.getModelPath();
             //获取最新500条数据
             List<Pollutant> pollutants = service.trainList(2500);
             //desc按照最新时间排序，第一个就是时间最新的
             Date date = pollutants.get(0).getCt();
             //获取从改时间开始的3个月90，模型预测5天，所以需要执行18次，将第一次的输出放进数组
             String[] fromNow = DateUtils.getFromNow(date, 24*90);
             int t = 0;
             double[] nums = new double[2500];
             ArrayList<Integer> arrayList = new ArrayList<>();
             while (t<18){
                 t++;
                 JSONObject jsonObject = new JSONObject();
                 if (t == 1){

                     for (int i = pollutants.size()-1; i >=0 ; i--) {
                         nums[i] = pollutants.get(i).getAqi();
                     }
                     System.out.println(modelPath);
                     jsonObject.putOpt("model_path",modelPath);
                     jsonObject.putOpt("nums",nums);
                 }else {
                     double[] numsNew = new double[nums.length+result.size()];
                     for (int i = 0; i < numsNew.length; i++) {
                         //数组拷贝
                         if (i<nums.length){
                             //先把原数组的数据全部复制给新数组
                             numsNew[i] = nums[i];
                         }else{
                             //剩余的值，通过当前下标-原数组长度获取下标
                             numsNew[i] = result.get(i-nums.length);
                         }
                     }
                     System.out.println(modelPath);
                     jsonObject.putOpt("model_path",modelPath);
                     jsonObject.putOpt("nums",numsNew);
                 }
                 jsonObject.putOpt("steps",120);
                 pyConnect.getConnection(PyConstant.PREDICT_PATH,jsonObject,PyConstant.POST);
                 pyConnect.write(jsonObject);
                 result = pyConnect.getResult();
                 arrayList.addAll(result);
                 System.out.println("结果集大小："+result.size());
                 System.out.println("结果集全部存到："+arrayList+","+"本轮存入："+result);
                 System.out.println("最终大小："+arrayList.size());
             }
             //key 形如 2001-4-24，对应的日期数组和数据
             map = new HashMap<>();
             String tomorrow = fromNow[0];
             ArrayList<String> dateList = new ArrayList<>();
             ArrayList<Integer> numList = new ArrayList<>();
             //时间类型yyyy-mm-dd hh:mm:ss 转yyyy-mm-dd
             String stringMD = DateUtils.toStringMD(tomorrow);
             for (int i = 0; i < fromNow.length; i++) {
                 System.out.println("标记："+stringMD);
                 if (stringMD.equals(DateUtils.toStringMD(fromNow[i]))){
                     dateList.add(fromNow[i]);
                     numList.add(arrayList.get(i));
                     if (i == fromNow.length-1){
                         PredictDataVo dataVo = new PredictDataVo();
                         System.out.println("numList:"+numList);
                         dataVo.setData(numList);
                         System.out.println("dateList:"+dateList);
                         dataVo.setDateList(dateList);
                         map.put(stringMD,dataVo);
                         //重置
                         numList = new ArrayList<>();
                         dateList = new ArrayList<>();
                         System.out.println("上一轮标记结果："+map);
                         stringMD = DateUtils.toStringMD(fromNow[i]);
                         System.out.println("标记移动：新标记"+stringMD);
                     }
                 }else {
                     PredictDataVo dataVo = new PredictDataVo();
                     System.out.println("numList:"+numList);
                     dataVo.setData(numList);
                     System.out.println("dateList:"+dateList);
                     dataVo.setDateList(dateList);
                     map.put(stringMD,dataVo);
                     //重置
                     numList = new ArrayList<>();
                     dateList = new ArrayList<>();
                     System.out.println("上一轮标记结果："+map);
                     stringMD = DateUtils.toStringMD(fromNow[i]);
                     System.out.println("标记移动：新标记"+stringMD);
                     //0点的不要忘记
                     numList.add(arrayList.get(i));
                     dateList.add(fromNow[i]);
                 }
             }
             double pre = 0;
             for (Map.Entry<String,Object> entry: map.entrySet()) {
                 String key = entry.getKey();
                 PredictDataVo value = (PredictDataVo) entry.getValue();
                 List<Integer> valueData = value.getData();
                 int max = valueData.get(0);
                 int min = valueData.get(0);
                 for (int i = 0; i < valueData.size(); i++) {
                     int mid = valueData.get(i);
                     if (mid>=max){
                         max = mid;
                         System.out.println(max);
                     }
                     int midMin = valueData.get(i);
                     if (midMin<=min){
                         min = midMin;
                     }
                 }
                 double s =  Math.random();
                 if (pre == 0){
                     pre = s;
                 }else {
                     if ((s>0 && s<0.1) || (s>0.3 && s<0.4) ||(s>0.7 && s<0.8) ){
                         s+=0.1;
                     }else {
                         s-=0.1;
                     }
                 }
                 if ( (s>0 && s<0.1) || (s>0.3 && s<0.4) ||(s>0.7 && s<0.8) ||  (s>0.9 && s<0.1) ){
                     map.put(key,max);
                 }else {
                     map.put(key,min);
                 }

             }
             Map<String,Object> map1 = new TreeMap<>(map);
             redisCache.setCacheMap("preMonth30*3",map1);
             redisCache.expire("preMonth30*3",8, TimeUnit.HOURS);
         }else {
             Map<String,Object> map1 = new TreeMap<>(map);
             System.out.println(map1);
         }

     }

     @Test
    public void testForPreMonth(){
         Map<String, Object> map = null;
         List<Integer> result = null;
         map = redisCache.getCacheMap("preMonth30");
         if (ObjectUtils.isEmpty(map)){
             PreModel model = preModelService.selectPreModelByName("DEFAULT");
             String modelPath = model.getModelPath();
             //获取最新500条数据
             List<Pollutant> pollutants = service.trainList(500);
             //desc按照最新时间排序，第一个就是时间最新的
             Date date = pollutants.get(0).getCt();
             //获取从改时间开始的10天，模型预测5天，所以需要执行2次，将第一次的输出放进数组
             String[] fromNow = DateUtils.getFromNow(date, 24*30);
             int t = 0;
             double[] nums = new double[500];
             ArrayList<Integer> arrayList = new ArrayList<>();
             while (t<6){
                 t++;
                 JSONObject jsonObject = new JSONObject();
                 if (t == 1){

                     for (int i = pollutants.size()-1; i >=0 ; i--) {
                         nums[i] = pollutants.get(i).getAqi();
                     }
                     System.out.println(modelPath);
                     jsonObject.putOpt("model_path",modelPath);
                     jsonObject.putOpt("nums",nums);
                     jsonObject.putOpt("steps",120);
                 }else {
                     double[] numsNew = new double[nums.length+result.size()];
                     for (int i = 0; i < numsNew.length; i++) {
                         //数组拷贝
                         if (i<nums.length){
                             //先把原数组的数据全部复制给新数组
                             numsNew[i] = nums[i];
                         }else{
                             //剩余的值，通过当前下标-原数组长度获取下标
                             numsNew[i] = result.get(i-nums.length);
                         }
                     }
                     System.out.println(modelPath);
                     jsonObject.putOpt("model_path",modelPath);
                     jsonObject.putOpt("nums",numsNew);
                     jsonObject.putOpt("steps",120);
                 }
                 pyConnect.getConnection(PyConstant.PREDICT_PATH,jsonObject,PyConstant.POST);
                 pyConnect.write(jsonObject);
                 result = pyConnect.getResult();
                 arrayList.addAll(result);
                 System.out.println("结果集大小："+result.size());
                 System.out.println("结果集全部存到："+arrayList+","+"本轮存入："+result);
                 System.out.println("最终大小："+arrayList.size());
             }
             //key 形如 2001-4-24，对应的日期数组和数据
             map = new HashMap<String, Object>();
             String tomorrow = fromNow[0];
             ArrayList<String> dateList = new ArrayList<>();
             ArrayList<Integer> numList = new ArrayList<>();
             String stringMD = DateUtils.toStringMD(tomorrow);
             for (int i = 0; i < fromNow.length; i++) {

                 System.out.println("标记："+stringMD);
                 if (stringMD.equals(DateUtils.toStringMD(fromNow[i]))){
                     dateList.add(fromNow[i]);
                     numList.add(arrayList.get(i));
                     if (i == fromNow.length-1){
                         PredictDataVo dataVo = new PredictDataVo();
                         System.out.println("numList:"+numList);
                         dataVo.setData(numList);
                         System.out.println("dateList:"+dateList);
                         dataVo.setDateList(dateList);
                         map.put(stringMD,dataVo);
                         //重置
                         numList = new ArrayList<>();
                         dateList = new ArrayList<>();
                         System.out.println("上一轮标记结果："+map);
                         stringMD = DateUtils.toStringMD(fromNow[i]);
                         System.out.println("标记移动：新标记"+stringMD);
                     }
                 }else {
                     PredictDataVo dataVo = new PredictDataVo();
                     System.out.println("numList:"+numList);
                     dataVo.setData(numList);
                     System.out.println("dateList:"+dateList);
                     dataVo.setDateList(dateList);
                     map.put(stringMD,dataVo);
                     //重置
                     numList = new ArrayList<>();
                     dateList = new ArrayList<>();
                     System.out.println("上一轮标记结果："+map);
                     stringMD = DateUtils.toStringMD(fromNow[i]);
                     System.out.println("标记移动：新标记"+stringMD);

                 }
             }
             redisCache.setCacheMap("preMonth30",map);
             redisCache.expire("preMonth30",8, TimeUnit.HOURS);
         }
         System.out.println(map);
     }
}