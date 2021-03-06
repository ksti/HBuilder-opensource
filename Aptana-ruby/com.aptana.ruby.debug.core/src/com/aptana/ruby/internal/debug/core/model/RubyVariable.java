package com.aptana.ruby.internal.debug.core.model;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;

import com.aptana.ruby.debug.core.RubyDebugCorePlugin;
import com.aptana.ruby.debug.core.model.IRubyStackFrame;
import com.aptana.ruby.debug.core.model.IRubyValue;
import com.aptana.ruby.debug.core.model.IRubyVariable;
import com.aptana.ruby.internal.debug.core.RubyDebuggerProxy;

public class RubyVariable extends RubyDebugElement implements IRubyVariable
{

	private boolean isStatic;
	private boolean isLocal;
	private boolean isInstance;
	private boolean isConstant;
	private IRubyStackFrame stackFrame;
	private String name;
	private String objectId;
	private IValue value;
	private IRubyVariable parent;
	private boolean valueHasChanged = false;

	public RubyVariable(IRubyStackFrame stackFrame, String name, String scope)
	{
		super(stackFrame.getDebugTarget());
		this.initialize(stackFrame, name, scope, null, new RubyValue(this));
	}

	public RubyVariable(IRubyStackFrame stackFrame, String name, String scope, String value, String type,
			boolean hasChildren, String objectId)
	{
		super(stackFrame.getDebugTarget());
		this.initialize(stackFrame, name, scope, objectId, new RubyValue(this, value, type, hasChildren));
	}

	protected final void initialize(IRubyStackFrame stackFrame, String name, String scope, String objectId,
			RubyValue value)
	{
		this.stackFrame = stackFrame;
		this.value = value;
		this.name = name;
		this.objectId = objectId;
		this.isStatic = scope.equals("class"); //$NON-NLS-1$
		this.isLocal = scope.equals("local"); //$NON-NLS-1$
		this.isInstance = scope.equals("instance"); //$NON-NLS-1$
		this.isConstant = scope.equals("constant"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.core.model.IVariable#getValue()
	 */
	public IValue getValue()
	{
		return value;
	}

	/**
	 * @see org.eclipse.debug.core.model.IVariable#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @see org.eclipse.debug.core.model.IVariable#getReferenceTypeName()
	 */
	public String getReferenceTypeName() throws DebugException
	{
		return getValue().getReferenceTypeName();
	}

	/**
	 * @see org.eclipse.debug.core.model.IVariable#hasValueChanged()
	 */
	public boolean hasValueChanged() throws DebugException
	{
		return valueHasChanged;
	}

	/**
	 * @see org.eclipse.debug.core.model.IValueModification#setValue(String)
	 */
	public void setValue(String expression) throws DebugException
	{
		try
		{
			String assignee = getName();
			if (isHashValue())
			{
				assignee = parent.getName() + "[" + assignee + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			else if (isArrayValue())
			{
				assignee = parent.getName() + assignee;
			}
			RubyVariable var = getRubyDebuggerProxy().readInspectExpression(stackFrame, assignee + " = " + expression); //$NON-NLS-1$
			this.value = var.getValue();
			this.valueHasChanged = true;
			fireChangeEvent(DebugEvent.CONTENT);
		}
		catch (RubyProcessingException e)
		{
			throw new DebugException(new Status(Status.ERROR, RubyDebugCorePlugin.PLUGIN_ID, -1, e.getMessage(), e));
		}
	}

	public RubyDebuggerProxy getRubyDebuggerProxy()
	{
		return ((RubyDebugTarget) this.getDebugTarget()).getRubyDebuggerProxy();
	}

	/**
	 * @see org.eclipse.debug.core.model.IValueModification#setValue(IValue)
	 */
	public void setValue(IValue value) throws DebugException
	{
		if (value instanceof RubyValue)
		{
			RubyValue val = (RubyValue) value;
			RubyVariable var = val.getOwner();
			setValue(var.getName()); // just do a basic assignment
		}
		else
		{
			setValue(value.getValueString());
		}
	}

	/**
	 * @see org.eclipse.debug.core.model.IValueModification#supportsValueModification()
	 */
	public boolean supportsValueModification()
	{
		return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.IValueModification#verifyValue(String)
	 */
	public boolean verifyValue(String expression) throws DebugException
	{
		// TODO Parse with a ruby parser and make sure we get back some value...
		// try
		// {
		// RubyParser parser = new RubyParser();
		// parser.parse(expression);
		// }
		// catch (SyntaxException e)
		// {
		// return false;
		// }
		return true;
	}

	/**
	 * @see org.eclipse.debug.core.model.IValueModification#verifyValue(IValue)
	 */
	public boolean verifyValue(IValue value) throws DebugException
	{
		return false;
	}

	public String toString()
	{
		if (this.isHashValue())
		{
			return this.getName() + " => " + this.getValue(); //$NON-NLS-1$
		}
		return this.getName() + " = " + this.getValue(); //$NON-NLS-1$

	}

	public IRubyStackFrame getStackFrame()
	{
		return stackFrame;
	}

	public IRubyVariable getParent()
	{
		return parent;
	}

	public void setParent(IRubyVariable parent)
	{
		this.parent = parent;
	}

	public String getQualifiedName()
	{
		if (parent == null)
		{
			return this.getName();
		}
		if (this.isHashValue())
		{
			if (((RubyValue) this.getValue()).getReferenceTypeName().equals("String")) //$NON-NLS-1$
			{
				return parent.getQualifiedName() + "[" + this.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return "[ObjectSpace._id2ref(" + this.getObjectId() + ")]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.getName().startsWith("[")) //$NON-NLS-1$
		{
			// Array
			return parent.getQualifiedName() + this.getName();
		}
		return parent.getQualifiedName() + "." + this.getName(); //$NON-NLS-1$
	}

	public boolean isInstance()
	{
		return isInstance;
	}

	public boolean isLocal()
	{
		return isLocal;
	}

	public boolean isStatic()
	{
		return isStatic;
	}

	public boolean isConstant()
	{
		return isConstant;
	}

	public String getObjectId()
	{
		return objectId;
	}

	public boolean isHashValue()
	{
		if (parent == null)
			return false;
		try
		{
			String type = ((IRubyValue) parent.getValue()).getReferenceTypeName();
			return type.equals("Hash") || type.equals("HashWithIndifferentAccess") //$NON-NLS-1$ //$NON-NLS-2$
					|| type.equals("ActionController::Flash::FlashHash"); //$NON-NLS-1$
		}
		catch (DebugException e)
		{
			return false;
		}

	}

	private boolean isArrayValue()
	{
		if (parent == null)
			return false;
		try
		{
			String type = ((IRubyValue) parent.getValue()).getReferenceTypeName();
			return type.equals("Array"); //$NON-NLS-1$
		}
		catch (DebugException e)
		{
			return false;
		}

	}

}
