import { prefUtil } from '../common/prefUtil'
import BackComponent from '../component/BackComponent'
import { postLogin,  } from '../http/api/user'


@Entry
@Component
struct Login {
  @State account:string ='admin'
  @State password:string = '123456'
  uiContext:UIContext=this.getUIContext()

  private showToast(mes:string,color?:ResourceColor){
    this.uiContext.getPromptAction().showToast({
      message:mes,
      textColor:color
    })
  }

  build() {
    Stack(){
      BackComponent(){
        Column({space:10}){
          Text('欢迎来到斗地主').font({size:26,weight:FontWeight.Bold}).fontColor(Color.Red)
          Text('准备好负债累累了吗').font({size:12})
          TextInput({text:this.account,placeholder:'请输入账号'})
            .onChange((value)=>{
            this.account=value
          })
          TextInput({text:this.password,placeholder:'请输入密码'}).onChange((value)=>{
            this.password=value
          }).type(InputType.Password)
          Button('登入').width("100%").type(ButtonType.Normal).borderRadius(5).backgroundColor(0xdc4639)
            .onClick(()=>{
              if (this.account==''||this.password==''){
                this.showToast('账号或密码为空',Color.Red)
                return
              }
              // postLogin
              postLogin(this.account,this.password).then((response)=>{
                console.info('postLogin AppTag'+JSON.stringify(response))
                if(response.code===1){
                  prefUtil.put('app','token',response.data.token)
                  this.uiContext.getRouter().replaceUrl({url:'pages/Index'})
                }else {
                  this.showToast(response.msg)
                }
              }).catch((error:Error)=>{
                console.error('postLogin Tag'+JSON.stringify(error))
              })
            })
          Row(){
            Button('注册新玩家').buttonExtend()
              .onClick(()=>{
                this.uiContext.getRouter().pushUrl({
                  url:'pages/Register'
                })
              })
            Button('找回密码').buttonExtend()
              .onClick(()=>{
                this.uiContext.getRouter().pushUrl({
                  url:'pages/Retrieve'
                })
              })
          }.justifyContent(FlexAlign.SpaceBetween).width('100%')
        }.width(300).height(300).justifyContent(FlexAlign.SpaceEvenly).padding(10)
      }
    }.height('100%').width('100%')
  }
}

@Extend(Button)
function buttonExtend(){
  .height(20)
  .fontSize(12)
  .buttonStyle(ButtonStyleMode.TEXTUAL)
}