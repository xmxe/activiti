package service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mapper.LeaveApplyMapper;

import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import po.LeaveApply;
import service.LeaveService;
@Transactional(propagation=Propagation.REQUIRED,isolation=Isolation.DEFAULT,timeout=5)
@Service
public class LeaveServiceImpl implements LeaveService{
	@Autowired
	LeaveApplyMapper leavemapper;
	@Autowired
	IdentityService identityservice;//在Activiti中内置了一套简单的对用户和用户组的支持，用于满足基本的业务需求。org.activiti.engine.identity该包用来进行身份管理和认证，其功能依托于IdentityService接口
	@Autowired
	RuntimeService runtimeservice;//使用RuntimeService提供的方法对流程进行控制
	@Autowired
	TaskService taskservice;//流程任务组件
	
	public ProcessInstance startWorkflow(LeaveApply apply, String userid, Map<String, Object> variables) {
		apply.setApply_time(new Date().toString());
		apply.setUser_id(userid);
		leavemapper.save(apply);//保存到leaveapply表,并且返回保存的主键id
		String businesskey=String.valueOf(apply.getId());//使用leaveapply表的主键作为businesskey,连接业务数据和流程数据
		System.out.println(businesskey);
		identityservice.setAuthenticatedUserId(userid);//设置当前线程的user_id
		System.out.println("当前线程UserId: " + Authentication.getAuthenticatedUserId());
		//开启流程 获取流程实例
		ProcessInstance instance=runtimeservice.startProcessInstanceByKey("leave",businesskey,variables);	
		String instanceid=instance.getId();
		apply.setProcess_instance_id(instanceid);
		leavemapper.update(apply);
		return instance;
	}

	public List<LeaveApply> getpagedepttask(String userid,int firstrow,int rowcount) {
		List<LeaveApply> results=new ArrayList<LeaveApply>();
		//根据用户组查询任务
		List<Task> tasks=taskservice.createTaskQuery().taskCandidateGroup("部门经理").listPage(firstrow, rowcount);		
		for(Task task:tasks){
			System.out.println(task.getId());//任务id
			Map<String, Object> taskMap = taskservice.getVariables(task.getId());//获取流程变量
			String instanceid=task.getProcessInstanceId();//获取流程实例id
			ProcessInstance ins=runtimeservice.createProcessInstanceQuery().processInstanceId(instanceid).singleResult();
			String businesskey=ins.getBusinessKey();//leaveapply主键id
			LeaveApply a=leavemapper.get(Integer.parseInt(businesskey));
			a.setTask(task);
			results.add(a);
		}
		return results;
	}
	
	public int getalldepttask(String userid) {
		List<Task> tasks=taskservice.createTaskQuery().taskCandidateGroup("部门经理").list();
		return tasks.size();
	}

	public LeaveApply getleave(int id) {
		LeaveApply leave=leavemapper.get(id);
		return leave;
	}

	public List<LeaveApply> getpagehrtask(String userid,int firstrow,int rowcount) {
		List<LeaveApply> results=new ArrayList<LeaveApply>();
		//根据用户组查询任务
		List<Task> tasks=taskservice.createTaskQuery().taskCandidateGroup("人事").listPage(firstrow, rowcount);
		for(Task task:tasks){
			//获取流程实例id
			String instanceid=task.getProcessInstanceId();
			//获取流程实例
			ProcessInstance ins=runtimeservice.createProcessInstanceQuery().processInstanceId(instanceid).singleResult();
			String businesskey=ins.getBusinessKey();//leaveapply主键id
			LeaveApply a=leavemapper.get(Integer.parseInt(businesskey));
			a.setTask(task);
			results.add(a);
		}
		return results;
	}

	public int getallhrtask(String userid) {
		List<Task> tasks=taskservice.createTaskQuery().taskCandidateGroup("人事").list();
		return tasks.size();
	}
	
	public List<LeaveApply> getpageXJtask(String userid,int firstrow,int rowcount) {
		List<LeaveApply> results=new ArrayList<LeaveApply>();
		//根据当前用户id查询任务
		List<Task> tasks=taskservice.createTaskQuery().taskCandidateOrAssigned(userid).taskName("销假").listPage(firstrow, rowcount);
		for(Task task:tasks){
			//获取流程实例id
			String instanceid=task.getProcessInstanceId();
			//获取流程实例
			ProcessInstance ins=runtimeservice.createProcessInstanceQuery().processInstanceId(instanceid).singleResult();
			String businesskey=ins.getBusinessKey();
			LeaveApply a=leavemapper.get(Integer.parseInt(businesskey));
			a.setTask(task);
			results.add(a);
		}
		return results;
	}

	public int getallXJtask(String userid) {
		List<Task> tasks=taskservice.createTaskQuery().taskCandidateOrAssigned(userid).taskName("销假").list();
		return tasks.size();
	}
	
	public List<LeaveApply> getpageupdateapplytask(String userid,int firstrow,int rowcount) {
		List<LeaveApply> results=new ArrayList<LeaveApply>();
		//根据当前用户id查询任务
		/*当使用taskCandidateOrAssigned做查询条件时，Activiti会按照以下规则查找Task：Assignee匹配或者*.bpmn中定义的Candidate Users 匹配
		或者Candidate Group 匹配（用户所属用户组的信息从Activiti的ACT_ID_*表获取）*/
		List<Task> tasks=taskservice.createTaskQuery().taskCandidateOrAssigned(userid).taskName("调整申请").listPage(firstrow, rowcount);
		for(Task task:tasks){
			String instanceid=task.getProcessInstanceId();
			ProcessInstance ins=runtimeservice.createProcessInstanceQuery().processInstanceId(instanceid).singleResult();
			String businesskey=ins.getBusinessKey();
			LeaveApply a=leavemapper.get(Integer.parseInt(businesskey));
			a.setTask(task);
			results.add(a);
		}
		return results;
	}
	
	public int getallupdateapplytask(String userid) {
		List<Task> tasks=taskservice.createTaskQuery().taskCandidateOrAssigned(userid).taskName("调整申请").list();
		return tasks.size();
	}
	
	public void completereportback(String taskid, String realstart_time, String realend_time) {
		//获取任务
		Task task=taskservice.createTaskQuery().taskId(taskid).singleResult();
		//获取流程实例id
		String instanceid=task.getProcessInstanceId();
		//获取流程实例
		ProcessInstance ins=runtimeservice.createProcessInstanceQuery().processInstanceId(instanceid).singleResult();
		//leaveapply主键
		String businesskey=ins.getBusinessKey();
		LeaveApply a=leavemapper.get(Integer.parseInt(businesskey));
		a.setReality_start_time(realstart_time);
		a.setReality_end_time(realend_time);
		leavemapper.update(a);
		//完成任务 执行下一步
		taskservice.complete(taskid);
	}

	public void updatecomplete(String taskid, LeaveApply leave,String reapply) {
		//获取任务
		Task task=taskservice.createTaskQuery().taskId(taskid).singleResult();
		//获取流程实例id
		String instanceid=task.getProcessInstanceId();
		//获取流程实例
		ProcessInstance ins=runtimeservice.createProcessInstanceQuery().processInstanceId(instanceid).singleResult();
		String businesskey=ins.getBusinessKey();
		LeaveApply a=leavemapper.get(Integer.parseInt(businesskey));
		a.setLeave_type(leave.getLeave_type());
		a.setStart_time(leave.getStart_time());
		a.setEnd_time(leave.getEnd_time());
		a.setReason(leave.getReason());
		Map<String,Object> variables=new HashMap<String,Object>();
		variables.put("reapply", reapply);//key对应.bpmn中定义的流程变量
		if(reapply.equals("true")){
			leavemapper.update(a);
			taskservice.complete(taskid,variables);
		}else
			taskservice.complete(taskid,variables);
	}
	
	public List<String> getHighLightedFlows(  
	        ProcessDefinitionEntity processDefinitionEntity,  
	        List<HistoricActivityInstance> historicActivityInstances) {  
	  
	    List<String> highFlows = new ArrayList<String>();// 用以保存高亮的线flowId  
	    for (int i = 0; i < historicActivityInstances.size(); i++) {// 对历史流程节点进行遍历  
	        ActivityImpl activityImpl = processDefinitionEntity  
	                .findActivity(historicActivityInstances.get(i)  
	                        .getActivityId());// 得 到节点定义的详细信息  
	        List<ActivityImpl> sameStartTimeNodes = new ArrayList<ActivityImpl>();// 用以保存后需开始时间相同的节点  
	        if ((i + 1) >= historicActivityInstances.size()) {  
	            break;  
	        }  
	        ActivityImpl sameActivityImpl1 = processDefinitionEntity  
	                .findActivity(historicActivityInstances.get(i + 1)  
	                        .getActivityId());// 将后面第一个节点放在时间相同节点的集合里  
	        sameStartTimeNodes.add(sameActivityImpl1);  
	        for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {  
	            HistoricActivityInstance activityImpl1 = historicActivityInstances  
	                    .get(j);// 后续第一个节点  
	            HistoricActivityInstance activityImpl2 = historicActivityInstances  
	                    .get(j + 1);// 后续第二个节点  
	            if (activityImpl1.getStartTime().equals(  
	                    activityImpl2.getStartTime())) {// 如果第一个节点和第二个节点开始时间相同保存  
	                ActivityImpl sameActivityImpl2 = processDefinitionEntity  
	                        .findActivity(activityImpl2.getActivityId());  
	                sameStartTimeNodes.add(sameActivityImpl2);  
	            } else {// 有不相同跳出循环  
	                break;  
	            }  
	        }  
	        List<PvmTransition> pvmTransitions = activityImpl  
	                .getOutgoingTransitions();// 取出节点的所有出去的线  
	        for (PvmTransition pvmTransition : pvmTransitions) {// 对所有的线进行遍历  
	            ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition  
	                    .getDestination();// 如果取出的线的目标节点存在时间相同的节点里，保存该线的id，进行高亮显示  
	            if (sameStartTimeNodes.contains(pvmActivityImpl)) {  
	                highFlows.add(pvmTransition.getId());  
	            }  
	        }  
	    }  
	    return highFlows;  
	}  
}
