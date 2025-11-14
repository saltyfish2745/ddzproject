import { prefUtil } from '../common/prefUtil'
import {
  getApplyForEmailcode,
  getApplyForEmailcodeFrombindingEmail,
  getClockIn,
  getPageBeanHistory,
  getPageBeanHistoryList,
  getViewUserInfo,
  putUpdateEmail,
  putUpdatePassword } from '../http/api/user'
import { JSON } from '@kit.ArkTS'

@Entry
@Component
struct Index {

  uiContext:UIContext=this.getUIContext()

  showToast(mes:string,color?:ResourceColor){
    this.uiContext.getPromptAction().showToast({
      message:mes,
      textColor:color?color:Color.Black
    })
  }

  token:string=''
  @State username:string=''
  @State beanCount:number=0

  @State buttonScale:number=1

  private updateUserInfo(){
    getViewUserInfo(this.token).then((data)=>{
      if (data.code==1) {
        this.username=data.data.username
        this.beanCount=data.data.beanCount
      }else {
        this.showToast(data.msg)
        this.getUIContext().getRouter().replaceUrl({url:'pages/Login'})
      }
    }).catch((e:Error)=>{
      this.showToast('登入生效\n重新登入')
      this.getUIContext().getRouter().replaceUrl({url:'pages/Login'})
    })
  }

  async aboutToAppear(): Promise<void> {
    //验证token
    this.token = await prefUtil.get('app','token') as string
    if(this.token && this.token!==''){
      this.updateUserInfo()
    }else {
      this.getUIContext().getRouter().replaceUrl({url:'pages/Login'})
    }
  }

  onPageShow(): void {
    //button呼吸动画
    this.getUIContext().animateTo({
      duration:1000,
      iterations:-1,
      curve:Curve.EaseInOut,
      playMode:PlayMode.Alternate
    },()=>{
      this.buttonScale=1.05
    })
  }

  @State showPageHistory:boolean=false

  build() {
    Stack(){
      Row(){
        Row({space:10}){
          Button('退出登入').buttonExtend('#D95E0B',this.buttonScale)
            .onClick(()=>{
              prefUtil.delete('app','token')
              this.uiContext.getRouter().replaceUrl({url:'pages/Login'})
            })
          Button('修改密码').buttonExtend('#4CA4FF',this.buttonScale)
            .onClick(()=>{
              //弹出更新密码对话框
              let controller = new CustomDialogController({
                builder:updatePasswordDialog({
                  token:this.token,
                  close:()=>{
                    controller.close()
                  }
                })
              })
              controller.open()
            })
          Button('更新邮箱').buttonExtend('#909399',this.buttonScale)
            .onClick(()=>{
              let controller = new CustomDialogController({
                builder:updateEmailDialog({
                  token:this.token,
                  close:()=>{
                    controller.close()
                  }
                })
              })
              controller.open()
            })
        }
        Row({space:10}){
          Button('查询历史记录').buttonExtend('#308BCC',this.buttonScale)
            .onClick(()=>{
              this.showPageHistory=true
            })

          Button('每日签到').buttonExtend('#2ABB67',this.buttonScale)
            .onClick(()=>{
              //签到请求
              getClockIn(this.token).then((data)=>{
                this.updateUserInfo()
                this.showToast(data.msg,data.code==1?Color.Black:Color.Red)
              })
            })

        }
      }.width('100%').position({y:0}).justifyContent(FlexAlign.SpaceBetween)

      Column(){

        Text(this.username).margin({top:70}).font({weight:FontWeight.Bold})

        Column(){
          Button('开始匹配').type(ButtonType.Circle).width(90).backgroundColor('#D54436')
            .scale({x:this.buttonScale,y:this.buttonScale})
        }.width(100).height(100)

        Row(){
          Image($rawfile('icon/dollar.svg')).fillColor('#FFD700').height(12).margin({right:10})
          Text(this.beanCount.toString()).backgroundColor(Color.White)
            .height(12).font({size:12,weight:FontWeight.Bold}).fontColor('#FFD700')
        }.shadow(ShadowStyle.OUTER_DEFAULT_XS).padding({left:20,right:20}).borderRadius(50).height(25)

      }.height('100%').justifyContent(FlexAlign.SpaceBetween)

      if(this.showPageHistory){
        Column(){
        }.width('100%').height('100%').onClick(()=>{
          this.showPageHistory=false
        })
        Column(){
          BeanHistory({token:this.token})
        }.width('70%').height("100%").backgroundColor('#FEFEFE')
        .shadow(ShadowStyle.OUTER_DEFAULT_XS).padding(10)
      }

    }.width('100%').height('100%').backgroundColor(Color.White).padding(20)
  }
}

@Extend(Button)
function buttonExtend(backcolor:ResourceColor,buttonScale:number){
  .height(30).width(100).backgroundColor(backcolor)
  .type(ButtonType.Normal).borderRadius(7)
  .scale({x:buttonScale,y:buttonScale})
}

@CustomDialog
struct updatePasswordDialog{
  controller?:CustomDialogController
  oldPassword:string=''
  newPassword:string=''
  @Require token:string=''
  @Require close:()=>void=()=>{}

  build() {
    Column(){
      Text('修改密码').font({size:27})
      TextInput({text:this.oldPassword,placeholder:'输入原密码'})
        .onChange((value)=>{
          this.oldPassword=value
        })

      TextInput({text:this.newPassword,placeholder:'输入新密码'})
        .onChange((value)=>{
          this.newPassword=value
        })
      Button('确认修改')
        .onClick(()=>{
          if(this.oldPassword==''||this.newPassword==''){
            this.getUIContext().getPromptAction().showToast({
              message:'输入错误'
            });return
          }

            putUpdatePassword(this.token,this.oldPassword,this.newPassword)
              .then((response)=>{
                console.info('putUpdatePassword Tag'+JSON.stringify(response))
                if(response.code==1){
                  this.getUIContext().getPromptAction().showToast({
                    message:response.msg
                  })
                  this.close()
                }else {
                  this.getUIContext().getPromptAction().showToast({
                    message:response.msg
                  })
                }
              }).catch((e:Error)=>{
                console.error('putUpdatePassword Tag'+JSON.stringify(e))
            })
        })
    }.height(230).justifyContent(FlexAlign.SpaceEvenly).padding(10)
  }
}

@CustomDialog
struct updateEmailDialog{
  controller?:CustomDialogController
  oldCode:string=''
  newEmail:string=''
  newCode:string=''
  @Require token:string=''
  @Require close:()=>void=()=>{}

  @State oldCountdown:number=0
  @State newCountdoen:number=0

  build() {
    Column(){
      Text('更新邮箱').font({size:27})
      Column(){
        Row(){
          Text('如果绑定了邮箱必须申请验证码').font({size:12}).fontColor('#7B7F88').margin({left:20})
        }.width('100%')
        Row(){
          TextInput({text:this.oldCode,placeholder:'原邮箱验证码（选填）'})
            .onChange((value)=>{
              this.oldCode=value
            }).layoutWeight(3).margin({right:20})
          Button(this.oldCountdown==0?'申请验证码':this.oldCountdown.toString()+'秒后重试').layoutWeight(1).enabled(this.oldCountdown==0?true:false)
            .onClick(()=>{
              //请求老邮箱验证码
              getApplyForEmailcodeFrombindingEmail(this.token).then((data)=>{
                if(data.code==1){
                  this.getUIContext().getPromptAction().showToast({message:"请求成功"})
                  this.oldCountdown=data.data
                  let timer=setInterval(()=>{
                    this.oldCountdown--
                    if(this.oldCountdown==0)
                      clearInterval(timer)
                  },1000)
                }else {
                  this.getUIContext().getPromptAction().showToast({message:data.msg})
                }
              }).catch((e:Error)=>{
                console.error('getApplyForEmailcodeFrombindingEmail Tag'+JSON.stringify(e))
              })
            })
        }.width('100%')
      }

      TextInput({text:this.newEmail,placeholder:'请输入新邮箱地址'})
        .onChange((value)=>{
          this.newEmail=value
        })
      Row(){
        TextInput({text:this.newCode,placeholder:'请输入新邮箱验证码'})
          .onChange((value)=>{
            this.newCode=value
          }).layoutWeight(3).margin({right:20})
        Button(this.newCountdoen==0?'申请验证码':this.newCountdoen.toString()+'秒后重试').layoutWeight(1)
          .onClick(()=>{
            if(!RegExp('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$').test(this.newEmail)){
              this.getUIContext().getPromptAction().showToast({message:'请输入正确邮箱'})
              return
            }
            //请求新邮箱验证码
            getApplyForEmailcode(this.newEmail).then((data)=>{
              if(data.code==1){
                this.getUIContext().getPromptAction().showToast({message:"请求成功"})
                this.newCountdoen=data.data
                let timer=setInterval(()=>{
                  this.newCountdoen--
                  if(this.newCountdoen==0)
                    clearInterval(timer)
                },1000)
              }else {
                this.getUIContext().getPromptAction().showToast({message:data.msg})
              }
            }).catch((e:Error)=>{
              console.error('getApplyForEmailcode Tag'+JSON.stringify(e))
            })
          })
      }
      Button('确认修改')
        .onClick(()=>{
          putUpdateEmail(this.token,this.newCode,this.newEmail,this.oldCode).then((data)=>{
            if(data.code==1){
              this.getUIContext().getPromptAction().showToast({message:data.msg})
              this.close()
            }else{
              this.getUIContext().getPromptAction().showToast({message:data.msg})
            }
          }).catch((e:Error)=>{
            console.error('putUpdateEmail Tag'+JSON.stringify(e))
          })
        })
    }.height('100%').justifyContent(FlexAlign.SpaceEvenly).padding(10)
  }
}

@ComponentV2
struct BeanHistory{
  @Require @Param token:string
  //分页查询接收的数据
  @Local beanInfos:getPageBeanHistoryList[]=[]

  total:number=99
  page:number=1
  pageSize:number=10

  private updateBeanInfos(){
    if(this.total>this.beanInfos.length){
      getPageBeanHistory(this.token,this.page,this.pageSize).then((data)=>{
        console.info('getPageBeanHistory Tag'+JSON.stringify(data))
        if(data.code==1){
          this.total=data.data.total
          this.beanInfos=[...this.beanInfos,...data.data.list]
          this.page++
        }else {
          this.getUIContext().getPromptAction().showToast({message:'请求失败'})
        }
      }).catch((e:Error)=>{
        this.getUIContext().getPromptAction().showToast({message:'请求失败'})
        console.error('getPageBeanHistory Tag'+JSON.stringify(e))
      })
    }
  }

  private getStrOnChangeType(changeType:string){
    switch (changeType){
      case 'CLOCK_IN':
        return '签到'
      case 'GAME':
        return '游戏'
      case 'REGISTER':
        return '注册'
      default :
        return '其他'
    }
  }

  build() {
    Column(){
      Text('欢乐豆历史记录').font({weight:FontWeight.Bold})
      Row(){
        Text('类型').layoutWeight(1).textAlign(TextAlign.Center)
        Text('变动数量').layoutWeight(1).textAlign(TextAlign.Center)
        Text('当前数量').layoutWeight(1).textAlign(TextAlign.Center)
        Text('时间').layoutWeight(2).textAlign(TextAlign.Center)
      }.width('100%')
      Divider().strokeWidth(1).color(Color.Gray)
      List(){
        ForEach(this.beanInfos,(item:getPageBeanHistoryList,index)=>{
          ListItem(){
            Row(){
              Text(this.getStrOnChangeType(item.changeType)).layoutWeight(1).textAlign(TextAlign.Center)
              Text(item.changeAmount>0?`+${item.changeAmount}`:item.changeAmount.toString()).layoutWeight(1)
                .textAlign(TextAlign.Center).fontColor(item.changeAmount<0?Color.Red:Color.Green)
              Text(item.currentBean.toString()).layoutWeight(1).textAlign(TextAlign.Center)
              Text(item.createTime).layoutWeight(2).textAlign(TextAlign.Center)
            }
          }.width('100%').height(50)
        })
      }.width('100%')
      .divider({
        strokeWidth:1,
        color:Color.Gray,
      })
      .onReachEnd(()=>{
        this.updateBeanInfos()
      })

    }.width('100%').height(270)
  }

}